package net.minesky.entities.item;

import net.minesky.utils.InteractionType;

public class ItemSkill {
    private String id;
    private InteractionType interactionType;
    private float cooldown;
    private String mythicSkillId;
    public ItemSkill(String id, InteractionType interactionType, float cooldown, String mythicSkillId) {
        this.id = id;
        this.interactionType = interactionType;
        this.cooldown = cooldown;
        this.mythicSkillId = mythicSkillId;
    }

    public String getId() {
        return id;
    }

    public float getCooldown() {
        return cooldown;
    }

    public String getMythicSkillId() {
        return mythicSkillId;
    }

    public InteractionType getInteractionType() {
        return interactionType;
    }

    public void setId(String id) {
        this.id = id;
    }

    public void setCooldown(float cooldown) {
        this.cooldown = cooldown;
    }

    public void setMythicSkillId(String mythicSkillId) {
        this.mythicSkillId = mythicSkillId;
    }

    public void setInteractionType(InteractionType interactionType) {
        this.interactionType = interactionType;
    }
}