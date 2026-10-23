package com.skillswap.bean;

import com.skillswap.dao.UserDAO;
import com.skillswap.model.User;

import javax.faces.application.FacesMessage;
import javax.faces.bean.ManagedBean;
import javax.faces.bean.SessionScoped;
import javax.faces.context.FacesContext;
import java.io.Serializable;

@ManagedBean(name = "userBean")
@SessionScoped
public class UserBean implements Serializable {
    private static final long serialVersionUID = 1L;

    private final UserDAO userDAO = new UserDAO();

    private User currentUser;

    // Registration inputs
    private String regUsn;
    private String regName;
    private String regEmail;
    private String regPassword;
    private String regDepartment;
    private String regBio;

    // Login inputs
    private String loginIdentifier; // USN or Email
    private String loginPassword;

    public UserBean() {}

    public String register() {
        if (regUsn == null || regUsn.trim().isEmpty() ||
            regName == null || regName.trim().isEmpty() ||
            regEmail == null || regEmail.trim().isEmpty() ||
            regPassword == null || regPassword.trim().isEmpty()) {
            addErrorMessage("Please fill in all required fields (USN, Name, Email, Password).");
            return null;
        }

        User newUser = new User();
        newUser.setUsn(regUsn.trim());
        newUser.setName(regName.trim());
        newUser.setEmail(regEmail.trim());
        newUser.setPassword(regPassword.trim());
        newUser.setDepartment(regDepartment != null ? regDepartment.trim() : "General");
        newUser.setBio(regBio != null ? regBio.trim() : "Student learner & mentor at NMAMIT.");
        newUser.setReputationScore(4.5);

        boolean success = userDAO.register(newUser);
        if (success) {
            this.currentUser = newUser;
            addInfoMessage("Registration successful! Welcome to SkillSwap, " + newUser.getName() + ".");
            // Reset fields
            regUsn = regName = regEmail = regPassword = regDepartment = regBio = "";
            return "dashboard?faces-redirect=true";
        } else {
            addErrorMessage("Registration failed: A student with this USN or Email already exists.");
            return null;
        }
    }

    public String login() {
        if (loginIdentifier == null || loginIdentifier.trim().isEmpty() ||
            loginPassword == null || loginPassword.trim().isEmpty()) {
            addErrorMessage("Please provide both USN/Email and Password.");
            return null;
        }

        User user = userDAO.login(loginIdentifier, loginPassword);
        if (user != null) {
            this.currentUser = user;
            addInfoMessage("Welcome back, " + user.getName() + "!");
            return "dashboard?faces-redirect=true";
        } else {
            addErrorMessage("Invalid credentials. Please verify your Email/USN and password.");
            return null;
        }
    }

    public String logout() {
        FacesContext.getCurrentInstance().getExternalContext().invalidateSession();
        this.currentUser = null;
        return "login?faces-redirect=true";
    }

    public String updateProfile() {
        if (currentUser == null) {
            return "login?faces-redirect=true";
        }
        boolean ok = userDAO.updateProfile(currentUser);
        if (ok) {
            addInfoMessage("Profile updated successfully!");
        } else {
            addErrorMessage("Failed to update profile. Please try again.");
        }
        return null;
    }

    public boolean isLoggedIn() {
        return currentUser != null;
    }

    private void addInfoMessage(String msg) {
        FacesContext.getCurrentInstance().addMessage(null, new FacesMessage(FacesMessage.SEVERITY_INFO, msg, null));
    }

    private void addErrorMessage(String msg) {
        FacesContext.getCurrentInstance().addMessage(null, new FacesMessage(FacesMessage.SEVERITY_ERROR, msg, null));
    }

    // Getters and Setters
    public User getCurrentUser() {
        // Pre-populate with Person 1 (Prithika) if no user logged in yet for testing
        if (currentUser == null) {
            currentUser = userDAO.getUserById(1);
        }
        return currentUser;
    }

    public void setCurrentUser(User currentUser) {
        this.currentUser = currentUser;
    }

    public String getRegUsn() {
        return regUsn;
    }

    public void setRegUsn(String regUsn) {
        this.regUsn = regUsn;
    }

    public String getRegName() {
        return regName;
    }

    public void setRegName(String regName) {
        this.regName = regName;
    }

    public String getRegEmail() {
        return regEmail;
    }

    public void setRegEmail(String regEmail) {
        this.regEmail = regEmail;
    }

    public String getRegPassword() {
        return regPassword;
    }

    public void setRegPassword(String regPassword) {
        this.regPassword = regPassword;
    }

    public String getRegDepartment() {
        return regDepartment;
    }

    public void setRegDepartment(String regDepartment) {
        this.regDepartment = regDepartment;
    }

    public String getRegBio() {
        return regBio;
    }

    public void setRegBio(String regBio) {
        this.regBio = regBio;
    }

    public String getLoginIdentifier() {
        return loginIdentifier;
    }

    public void setLoginIdentifier(String loginIdentifier) {
        this.loginIdentifier = loginIdentifier;
    }

    public String getLoginPassword() {
        return loginPassword;
    }

    public void setLoginPassword(String loginPassword) {
        this.loginPassword = loginPassword;
    }
}
