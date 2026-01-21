package com.ai.haha.springaistudyservice.service.moyoo.service.impl;

import com.ai.haha.springaistudyservice.service.moyoo.dto.MoYooReportDTO;
import com.ai.haha.springaistudyservice.service.moyoo.entity.MoYooRecord;
import com.ai.haha.springaistudyservice.service.moyoo.entity.MoYooResult;
import com.ai.haha.springaistudyservice.service.moyoo.entity.User;
import com.ai.haha.springaistudyservice.service.moyoo.enums.UserStatus;
import com.ai.haha.springaistudyservice.service.moyoo.mapper.MoYooRecordMapper;
import com.ai.haha.springaistudyservice.service.moyoo.mapper.MoYooResultMapper;
import com.ai.haha.springaistudyservice.service.moyoo.mapper.UserMapper;
import com.ai.haha.springaistudyservice.service.moyoo.service.MoYooService;
import com.ai.haha.springaistudyservice.service.moyoo.statemachine.UserStatusStateMachine;
import com.ai.haha.springaistudyservice.service.moyoo.util.SnowflakeIdGenerator;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.time.LocalDateTime;

/**
 * 摸鱼服务实现类
 */
@Service
public class MoYooServiceImpl implements MoYooService {
    
    @Resource
    private MoYooRecordMapper recordMapper;
    
    @Resource
    private MoYooResultMapper resultMapper;
    
    @Resource
    private UserMapper userMapper;
    
    @Resource
    private SnowflakeIdGenerator idGenerator;
    
    @Resource
    private MoYooAIService aiService;
    
    @Override
    @Transactional
    public MoYooRecord startMoYoo(Long userId) {
        // 检查是否有未结束的记录
        MoYooRecord existingRecord = recordMapper.selectFirstByUserIdAndEndTimeIsNull(userId);
        if (existingRecord != null) {
            throw new RuntimeException("您还有未结束的摸鱼记录，请先结束之前的摸鱼");
        }
        
        // 使用状态机更新用户状态为摸鱼中
        User user = userMapper.selectByUserId(userId);
        if (user == null) {
            throw new RuntimeException("用户不存在，userId: " + userId);
        }
        UserStatus currentStatus = UserStatus.fromCode(user.getStatus());
        UserStatus newStatus = UserStatusStateMachine.startMoYoo(currentStatus);
        user.setStatus(newStatus.getCode());
        user.setUpdateTime(LocalDateTime.now());
        userMapper.updateById(user);
        
        MoYooRecord record = new MoYooRecord();
        record.setRecordId(idGenerator.nextId());
        record.setUserId(userId);
        record.setStartTime(LocalDateTime.now());
        record.setCreateTime(LocalDateTime.now());
        record.setUpdateTime(LocalDateTime.now());
        
        recordMapper.insert(record);
        return record;
    }
    
    @Override
    @Transactional(timeout = 30) // 设置事务超时时间为30秒
    public MoYooReportDTO endMoYoo(Long userId, Double durationMinutes) {
        MoYooRecord record = recordMapper.selectFirstByUserIdAndEndTimeIsNull(userId);
        if (record == null) {
            throw new RuntimeException("没有进行中的摸鱼记录");
        }
        
        // 使用前端提交的时长，不再计算
        if (durationMinutes == null || durationMinutes < 0) {
            throw new RuntimeException("摸鱼时长无效");
        }
        
        LocalDateTime endTime = LocalDateTime.now();
        record.setEndTime(endTime);
        record.setDurationMinutes(durationMinutes);
        record.setUpdateTime(LocalDateTime.now());
        
        recordMapper.updateById(record);
        
        // 获取用户信息
        User user = userMapper.selectByUserId(userId);
        if (user == null) {
            throw new RuntimeException("用户不存在，userId: " + userId);
        }
        
        // 计算碳排放和寿命增加
        double carbonReduction = calculateCarbonReduction(user, durationMinutes);
        double lifeExtension = calculateLifeExtension(durationMinutes);
        
        // 创建摸鱼成果（先不包含AI报告，避免事务时间过长）
        MoYooResult result = new MoYooResult();
        result.setResultId(idGenerator.nextId());
        result.setRecordId(record.getRecordId());
        result.setUserId(userId);
        result.setDurationMinutes(durationMinutes);
        result.setCarbonReductionGrams(carbonReduction);
        result.setLifeExtensionMinutes(lifeExtension);
        result.setAiReport(null); // 先设置为null，事务外生成
        result.setCreateTime(LocalDateTime.now());
        result.setUpdateTime(LocalDateTime.now());
        
        resultMapper.insert(result);
        
        // 使用状态机更新用户状态为在线
        UserStatus currentStatus = UserStatus.fromCode(user.getStatus());
        UserStatus newStatus = UserStatusStateMachine.endMoYoo(currentStatus);
        user.setStatus(newStatus.getCode());
        user.setUpdateTime(LocalDateTime.now());
        userMapper.updateById(user);
        
        // 保存resultId用于后续更新AI报告
        Long resultId = result.getResultId();
        
        // 构建返回DTO（先不包含AI报告，事务外生成）
        MoYooReportDTO reportDTO = new MoYooReportDTO();
        reportDTO.setRecordId(record.getRecordId());
        reportDTO.setDurationMinutes(durationMinutes);
        reportDTO.setCarbonReductionGrams(carbonReduction);
        reportDTO.setLifeExtensionMinutes(lifeExtension);
        reportDTO.setAiReport(null); // 先设置为null，事务外生成
        
        // 使用TransactionSynchronizationManager在事务提交后执行AI报告生成
        // 这样可以确保AI报告生成在事务外执行，避免长时间持有数据库锁
        if (TransactionSynchronizationManager.isActualTransactionActive()) {
            TransactionSynchronizationManager.registerSynchronization(
                new TransactionSynchronization() {
                    @Override
                    public void afterCommit() {
                        // 事务提交后，在事务外生成AI报告
                        generateAndUpdateAiReportAsync(resultId, user, durationMinutes, carbonReduction, lifeExtension, reportDTO);
                    }
                }
            );
        } else {
            // 如果没有活动事务，直接执行
            generateAndUpdateAiReportAsync(resultId, user, durationMinutes, carbonReduction, lifeExtension, reportDTO);
        }
        
        return reportDTO;
    }
    
    /**
     * 在事务外生成AI报告并更新数据库
     */
    private void generateAndUpdateAiReportAsync(Long resultId, User user, Double durationMinutes, 
                                                 double carbonReduction, double lifeExtension, 
                                                 MoYooReportDTO reportDTO) {
        // 使用新线程异步生成AI报告，避免阻塞主流程和持有数据库锁
        new Thread(() -> {
            try {
                // 生成AI报告（在事务外，不持有数据库锁）
                String aiReport = aiService.generateReport(user, durationMinutes, carbonReduction, lifeExtension);
                
                // 更新DTO中的AI报告（用于返回）
                // 注意：由于是异步执行，前端可能已经接收到响应，这里更新DTO可能无效
                // 但可以更新数据库中的AI报告
                reportDTO.setAiReport(aiReport);
                
                // 异步更新AI报告到数据库
                MoYooResult result = resultMapper.selectById(resultId);
                if (result != null) {
                    result.setAiReport(aiReport);
                    result.setUpdateTime(LocalDateTime.now());
                    resultMapper.updateById(result);
                }
            } catch (Exception e) {
                // 如果AI报告生成失败，使用默认报告
                String defaultReport = String.format(
                    "恭喜！您本次摸鱼%.2f分钟，减少碳排放%.2f克，寿命增加%.2f分钟。继续加油！",
                    durationMinutes, carbonReduction, lifeExtension
                );
                reportDTO.setAiReport(defaultReport);
                
                // 仍然尝试更新到数据库
                try {
                    MoYooResult result = resultMapper.selectById(resultId);
                    if (result != null) {
                        result.setAiReport(defaultReport);
                        result.setUpdateTime(LocalDateTime.now());
                        resultMapper.updateById(result);
                    }
                } catch (Exception ex) {
                    System.err.println("更新AI报告失败: " + ex.getMessage());
                }
            }
        }).start();
        
        // 为了前端能立即返回，先设置一个默认报告
        // AI报告生成完成后会异步更新
        String defaultReport = String.format(
            "正在生成AI报告，请稍候...\n您本次摸鱼%d分钟，减少碳排放%.2f克，寿命增加%.2f分钟。",
            durationMinutes, carbonReduction, lifeExtension
        );
        reportDTO.setAiReport(defaultReport);
    }
    
    @Override
    public MoYooRecord getCurrentMoYoo(Long userId) {
        return recordMapper.selectFirstByUserIdAndEndTimeIsNull(userId);
    }
    
    /**
     * 计算减少的碳排放（克）
     * 根据职业不同，碳排放系数不同
     */
    private double calculateCarbonReduction(User user, double durationMinutes) {
        // 基础碳排放系数（克/分钟）
        double baseCarbonPerMinute = 2.0; // 默认值
        
        // 根据职业调整系数
        if (user.getProfession() != null) {
            com.ai.haha.springaistudyservice.service.moyoo.enums.Profession profession = 
                com.ai.haha.springaistudyservice.service.moyoo.enums.Profession.fromChineseName(user.getProfession());
            
            switch (profession) {
                case PROGRAMMER:
                    baseCarbonPerMinute = 3.0; // 电脑运行耗电更多
                    break;
                case DESIGNER:
                    baseCarbonPerMinute = 2.5;
                    break;
                case PRODUCT_MANAGER:
                    baseCarbonPerMinute = 1.5;
                    break;
                case TESTER:
                    baseCarbonPerMinute = 2.8;
                    break;
                case OPERATION:
                case MARKETING:
                case SALES:
                    baseCarbonPerMinute = 1.8;
                    break;
                case HR:
                case FINANCE:
                    baseCarbonPerMinute = 1.5;
                    break;
                default:
                    baseCarbonPerMinute = 2.0;
            }
        }
        
        return baseCarbonPerMinute * durationMinutes;
    }
    
    /**
     * 计算寿命增加（分钟）
     * 基于研究：每减少1小时工作压力，可增加约5分钟寿命
     */
    private double calculateLifeExtension(double durationMinutes) {
        // 每10分钟摸鱼，增加约0.83分钟寿命（5分钟/60分钟）
        return durationMinutes * 0.083;
    }
}

