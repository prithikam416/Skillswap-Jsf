package com.skillswap.bean;

import com.skillswap.dao.SkillDAO;
import com.skillswap.model.UserSkill;

import javax.faces.application.FacesMessage;
import javax.faces.bean.ManagedBean;
import javax.faces.bean.SessionScoped;
import javax.faces.context.FacesContext;
import java.io.Serializable;
import java.util.List;

@ManagedBean(name = "skillBean")
@SessionScoped
public class SkillBean implements Serializable {
    private static final long serialVersionUID = 1L;

    private final SkillDAO skillDAO = new SkillDAO();

    private String newSkillName;
    private String newSkillType = "TEACH"; // Default to "Skills I Can Teach"
    private String newProficiency = "Intermediate";

    public SkillBean() {}

    public String addSkill(int userId) {
        if (newSkillName == null || newSkillName.trim().isEmpty()) {
            addErrorMessage("Please specify a valid skill name.");
            return null;
        }

        boolean ok = skillDAO.addSkill(userId, newSkillName.trim(), newSkillType, newProficiency);
        if (ok) {
            String label = newSkillType.equals("TEACH") ? "Offered (Teach)" : "Wanted (Learn)";
            addInfoMessage("Added '" + newSkillName.trim() + "' (" + newProficiency + ") to your " + label + " skills!");
            newSkillName = "";
        } else {
            addErrorMessage("Could not add skill: Already in your list or invalid input.");
        }
        return null;
    }

    public String quickAddSkill(int userId, String skillName, String skillType, String proficiency) {
        skillDAO.addSkill(userId, skillName, skillType, proficiency);
        addInfoMessage("Added " + skillName + " (" + proficiency + ") to your skills!");
        return null;
    }

    public String deleteSkill(int skillId) {
        boolean ok = skillDAO.deleteSkill(skillId);
        if (ok) {
            addInfoMessage("Skill removed successfully.");
        } else {
            addErrorMessage("Failed to remove skill.");
        }
        return null;
    }

    public List<UserSkill> getSkillsOffered(int userId) {
        return skillDAO.getUserSkills(userId, "TEACH");
    }

    public List<UserSkill> getSkillsWanted(int userId) {
        return skillDAO.getUserSkills(userId, "LEARN");
    }

    private void addInfoMessage(String msg) {
        FacesContext.getCurrentInstance().addMessage(null, new FacesMessage(FacesMessage.SEVERITY_INFO, msg, null));
    }

    private void addErrorMessage(String msg) {
        FacesContext.getCurrentInstance().addMessage(null, new FacesMessage(FacesMessage.SEVERITY_ERROR, msg, null));
    }

    // Getters and Setters
    public String getNewSkillName() {
        return newSkillName;
    }

    public void setNewSkillName(String newSkillName) {
        this.newSkillName = newSkillName;
    }

    public String getNewSkillType() {
        return newSkillType;
    }

    public void setNewSkillType(String newSkillType) {
        this.newSkillType = newSkillType;
    }

    public String getNewProficiency() {
        return newProficiency;
    }

    public void setNewProficiency(String newProficiency) {
        this.newProficiency = newProficiency;
    }
}
