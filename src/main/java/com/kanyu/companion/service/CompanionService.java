package com.kanyu.companion.service;

import com.kanyu.companion.model.CompanionProfile;
import com.kanyu.companion.model.PersonalityTraits;
import com.kanyu.companion.repository.CompanionProfileRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class CompanionService {
    
    private final CompanionProfileRepository profileRepository;
    
    @Transactional
    public CompanionProfile createProfile(Long userId, String name, String personality, 
                                          String speakingStyle, String relationship) {
        log.info("Creating companion profile for user: {}", userId);
        
        if (profileRepository.existsByUserId(userId)) {
            throw new IllegalStateException("Profile already exists for user: " + userId);
        }
        
        CompanionProfile profile = new CompanionProfile();
        profile.setUserId(userId);
        profile.setName(name != null ? name : "小助手");
        profile.setPersonality(personality != null ? personality : "温柔体贴，善解人意");
        profile.setSpeakingStyle(speakingStyle != null ? speakingStyle : "温暖友好");
        profile.setRelationship(relationship != null ? relationship : "朋友");
        profile.setPersonalityTraits(new PersonalityTraits());
        
        return profileRepository.save(profile);
    }
    
    @Transactional
    public CompanionProfile updateProfile(Long userId, Map<String, Object> updates) {
        log.info("Updating companion profile for user: {}", userId);
        
        CompanionProfile profile = profileRepository.findByUserId(userId)
            .orElseThrow(() -> new IllegalArgumentException("Profile not found for user: " + userId));
        
        if (updates.containsKey("name")) {
            profile.setName((String) updates.get("name"));
        }
        if (updates.containsKey("personality")) {
            profile.setPersonality((String) updates.get("personality"));
        }
        if (updates.containsKey("speakingStyle")) {
            profile.setSpeakingStyle((String) updates.get("speakingStyle"));
        }
        if (updates.containsKey("relationship")) {
            profile.setRelationship((String) updates.get("relationship"));
        }
        if (updates.containsKey("customRules")) {
            profile.setCustomRules((String) updates.get("customRules"));
        }
        if (updates.containsKey("personalityTraits")) {
            @SuppressWarnings("unchecked")
            Map<String, Object> traitsMap = (Map<String, Object>) updates.get("personalityTraits");
            PersonalityTraits traits = profile.getPersonalityTraits();
            if (traits == null) {
                traits = new PersonalityTraits();
            }
            if (traitsMap.containsKey("humor")) {
                traits.setHumor(((Number) traitsMap.get("humor")).floatValue());
            }
            if (traitsMap.containsKey("warmth")) {
                traits.setWarmth(((Number) traitsMap.get("warmth")).floatValue());
            }
            if (traitsMap.containsKey("proactivity")) {
                traits.setProactivity(((Number) traitsMap.get("proactivity")).floatValue());
            }
            if (traitsMap.containsKey("empathy")) {
                traits.setEmpathy(((Number) traitsMap.get("empathy")).floatValue());
            }
            if (traitsMap.containsKey("playfulness")) {
                traits.setPlayfulness(((Number) traitsMap.get("playfulness")).floatValue());
            }
            profile.setPersonalityTraits(traits);
        }
        
        return profileRepository.save(profile);
    }
    
    public Optional<CompanionProfile> getProfile(Long userId) {
        return profileRepository.findByUserId(userId);
    }
    
    public CompanionProfile getOrCreateDefaultProfile(Long userId) {
        return profileRepository.findByUserId(userId)
            .orElseGet(() -> createProfile(userId, "小助手", "温柔体贴", "温暖友好", "朋友"));
    }
    
    public String buildSystemPrompt(Long userId) {
        CompanionProfile profile = getOrCreateDefaultProfile(userId);
        
        StringBuilder prompt = new StringBuilder();
        
        prompt.append("你是").append(profile.getName()).append("，");
        prompt.append("是用户的").append(profile.getRelationship()).append("。\n\n");
        
        prompt.append("你的性格特点：").append(profile.getPersonality()).append("\n\n");
        
        prompt.append("你的说话风格：").append(profile.getSpeakingStyle()).append("\n\n");
        
        if (profile.getPersonalityTraits() != null) {
            prompt.append(profile.getPersonalityTraits().generatePersonalityPrompt()).append("\n");
        }
        
        if (profile.getCustomRules() != null && !profile.getCustomRules().isEmpty()) {
            prompt.append("\n特殊规则：\n").append(profile.getCustomRules()).append("\n");
        }
        
        prompt.append("\n请始终保持这个角色设定，用符合你性格和风格的方式与用户交流。");
        prompt.append("记住用户的重要信息，在适当的时候提及。");
        prompt.append("关心用户的情绪，在用户需要时给予支持和鼓励。");
        
        return prompt.toString();
    }
    
    @Transactional
    public void updatePreferences(Long userId, Map<String, Object> preferences) {
        CompanionProfile profile = getOrCreateDefaultProfile(userId);
        if (profile.getPreferences() == null) {
            profile.setPreferences(new java.util.HashMap<>());
        }
        profile.getPreferences().putAll(preferences);
        profileRepository.save(profile);
    }
}
