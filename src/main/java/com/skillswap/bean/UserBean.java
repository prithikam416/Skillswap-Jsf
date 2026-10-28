package com.skillswap.bean;

import com.skillswap.dao.UserDAO;
import com.skillswap.model.User;

import javax.faces.application.FacesMessage;
import javax.faces.bean.ManagedBean;
import javax.faces.bean.SessionScoped;
import javax.faces.context.FacesContext;
import javax.servlet.http.HttpServletRequest;
import java.io.Serializable;
import java.util.regex.Pattern;

@ManagedBean(name = "userBean")
@SessionScoped
public class UserBean implements Serializable {
    private static final long serialVersionUID = 1L;

    private static final Pattern EMAIL_PATTERN = Pattern.compile("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$");

    private final UserDAO userDAO = new UserDAO();

    private User currentUser;

    // Registration inputs
    private String regUsn;
    private String regName;
    private String regEmail;
    private String regPassword;
    private String regConfirmPassword;
    private String regDepartment;
    private String regBio;

    // Login inputs
    private String loginIdentifier; // USN or Email
    private String loginPassword;

    public UserBean() {}

    /**
     * Complete Student Registration Process:
     * - Validates every required field
     * - Validates email format
     * - Verifies passwords match and meet minimum strength
     * - Prevents duplicate USN and Email
     * - Enforces STUDENT role (prevents self-assignment of admin privileges)
     * - Hashes password securely
     * - Stores in MySQL database
     * - Redirects newly registered students directly to login
     */
    public String register() {
        // 1. Required field validation
        if (regUsn == null || regUsn.trim().isEmpty()) {
            addErrorMessage("Student USN / ID is required.");
            return null;
        }
        if (regName == null || regName.trim().isEmpty()) {
            addErrorMessage("Full Name is required.");
            return null;
        }
        if (regEmail == null || regEmail.trim().isEmpty()) {
            addErrorMessage("College Email is required.");
            return null;
        }
        if (regPassword == null || regPassword.trim().isEmpty()) {
            addErrorMessage("Password is required.");
            return null;
        }
        if (regConfirmPassword == null || regConfirmPassword.trim().isEmpty()) {
            addErrorMessage("Please confirm your password.");
            return null;
        }

        // 2. Email format validation
        String cleanEmail = regEmail.trim();
        if (!EMAIL_PATTERN.matcher(cleanEmail).matches()) {
            addErrorMessage("Please enter a valid email address (e.g., student@college.edu).");
            return null;
        }

        // 3. Password length validation
        if (regPassword.trim().length() < 6) {
            addErrorMessage("Password must be at least 6 characters in length.");
            return null;
        }

        // 4. Passwords match validation
        if (!regPassword.trim().equals(regConfirmPassword.trim())) {
            addErrorMessage("Passwords do not match. Please re-enter.");
            return null;
        }

        // 5. Uniqueness validation: Duplicate USN / ID
        String cleanUsn = regUsn.trim();
        if (userDAO.isUsnExists(cleanUsn)) {
            addErrorMessage("A student with USN '" + cleanUsn + "' is already registered.");
            return null;
        }

        // 6. Uniqueness validation: Duplicate Email
        if (userDAO.isEmailExists(cleanEmail)) {
            addErrorMessage("The email address '" + cleanEmail + "' is already in use.");
            return null;
        }

        // 7. Assemble user entity
        User newUser = new User();
        newUser.setUsn(cleanUsn);
        newUser.setName(regName.trim());
        newUser.setEmail(cleanEmail);
        newUser.setPassword(regPassword.trim()); // UserDAO will hash using PasswordUtil
        newUser.setDepartment(regDepartment != null && !regDepartment.trim().isEmpty() ? regDepartment.trim() : "Computer Science & Engineering");
        newUser.setBio(regBio != null ? regBio.trim() : "Student learner & mentor.");
        newUser.setReputationScore(0.0);
        newUser.setRole("STUDENT"); // Strictly enforce student role
        newUser.setActive(true);

        boolean success = userDAO.register(newUser);
        if (success) {
            // Reset registration fields
            regUsn = regName = regEmail = regPassword = regConfirmPassword = regDepartment = regBio = "";
            return "login?faces-redirect=true&registered=true";
        } else {
            addErrorMessage("Registration failed due to a database error. Please try again.");
            return null;
        }
    }

    /**
     * Primary Login Action:
     * - Validates non-empty input
     * - Authenticates against real database with hashed password check
     * - Checks account active status
     * - Defends against session fixation
     * - Routes students to dashboard and admins to admin dashboard
     */
    public String login() {
        if (loginIdentifier == null || loginIdentifier.trim().isEmpty()) {
            addErrorMessage("Please enter your USN or College Email.");
            return null;
        }
        if (loginPassword == null || loginPassword.trim().isEmpty()) {
            addErrorMessage("Please enter your password.");
            return null;
        }

        User user = userDAO.login(loginIdentifier.trim(), loginPassword.trim());
        if (user != null) {
            if (!user.isActive()) {
                addErrorMessage("Your account has been deactivated. Please contact the administrator.");
                return null;
            }

            // Defend against session fixation
            try {
                HttpServletRequest request = (HttpServletRequest) FacesContext.getCurrentInstance()
                        .getExternalContext().getRequest();
                if (request != null) {
                    request.changeSessionId();
                }
            } catch (Exception e) {
                // Ignore if container doesn't support changeSessionId
            }

            this.currentUser = user;
            this.loginPassword = ""; // Clear plain text password from memory

            // Save in session map for AuthFilter access
            FacesContext.getCurrentInstance().getExternalContext().getSessionMap().put("currentUser", user);

            if (currentUser.isAdmin()) {
                return "admin_dashboard?faces-redirect=true";
            }
            return "dashboard?faces-redirect=true";
        } else {
            addErrorMessage("Invalid credentials. Please verify your Email/USN and password.");
            return null;
        }
    }

    /**
     * Logs out the user, invalidates server session, and redirects to login.
     */
    public String logout() {
        try {
            FacesContext.getCurrentInstance().getExternalContext().getSessionMap().remove("currentUser");
            FacesContext.getCurrentInstance().getExternalContext().invalidateSession();
        } catch (Exception e) {
            // Ignore
        }
        this.currentUser = null;
        this.loginIdentifier = "";
        this.loginPassword = "";
        return "login?faces-redirect=true";
    }

    public boolean isLoggedIn() {
        return currentUser != null;
    }

    public boolean isAdmin() {
        return currentUser != null && currentUser.isAdmin();
    }

    public String updateProfile() {
        if (currentUser == null) {
            return "login?faces-redirect=true";
        }
        if (currentUser.getName() == null || currentUser.getName().trim().isEmpty()) {
            addErrorMessage("Full Name cannot be empty.");
            return null;
        }

        boolean ok = userDAO.updateProfile(currentUser);
        if (ok) {
            // Refresh current user data from database
            User refreshed = userDAO.getUserById(currentUser.getId());
            if (refreshed != null) {
                this.currentUser = refreshed;
                FacesContext.getCurrentInstance().getExternalContext().getSessionMap().put("currentUser", refreshed);
            }
            addInfoMessage("Profile updated successfully!");
        } else {
            addErrorMessage("Failed to update profile. Please try again.");
        }
        return null;
    }

    private void addInfoMessage(String msg) {
        FacesContext.getCurrentInstance().addMessage(null, new FacesMessage(FacesMessage.SEVERITY_INFO, msg, null));
    }

    private void addErrorMessage(String msg) {
        FacesContext.getCurrentInstance().addMessage(null, new FacesMessage(FacesMessage.SEVERITY_ERROR, msg, null));
    }

    // Getters and Setters
    public User getCurrentUser() {
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

    public String getRegConfirmPassword() {
        return regConfirmPassword;
    }

    public void setRegConfirmPassword(String regConfirmPassword) {
        this.regConfirmPassword = regConfirmPassword;
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
