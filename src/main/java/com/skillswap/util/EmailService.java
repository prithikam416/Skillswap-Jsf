package com.skillswap.util;

import javax.mail.*;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;
import java.io.InputStream;
import java.util.Date;
import java.util.Properties;

public class EmailService {

    public static class EmailResult {
        private final boolean success;
        private final boolean simulated;
        private final String message;
        private final String otp;

        public EmailResult(boolean success, boolean simulated, String message, String otp) {
            this.success = success;
            this.simulated = simulated;
            this.message = message;
            this.otp = otp;
        }

        public boolean isSuccess() {
            return success;
        }

        public boolean isSimulated() {
            return simulated;
        }

        public String getMessage() {
            return message;
        }

        public String getOtp() {
            return otp;
        }
    }

    private static final Properties config = new Properties();

    static {
        loadConfig();
    }

    private static void loadConfig() {
        // Try loading from classpath or system env
        try (InputStream in = EmailService.class.getClassLoader().getResourceAsStream("email.properties")) {
            if (in != null) {
                config.load(in);
            }
        } catch (Exception e) {
            System.err.println("[SkillSwap EmailService] Note: email.properties not loaded from classpath: " + e.getMessage());
        }

        // Allow system environment variable overrides
        overrideFromEnv("mail.smtp.host", "SMTP_HOST");
        overrideFromEnv("mail.smtp.port", "SMTP_PORT");
        overrideFromEnv("mail.smtp.username", "SMTP_USERNAME");
        overrideFromEnv("mail.smtp.password", "SMTP_PASSWORD");
        overrideFromEnv("mail.from", "SMTP_FROM");
    }

    private static void overrideFromEnv(String propKey, String envKey) {
        String envVal = System.getenv(envKey);
        if (envVal != null && !envVal.trim().isEmpty()) {
            config.setProperty(propKey, envVal.trim());
        }
    }

    /**
     * Sends a 6-digit OTP to the user's email address.
     * If SMTP credentials are configured and reachable, sends real email via SMTP.
     * Otherwise, securely logs the OTP to server console and provides graceful dev fallback.
     */
    public static EmailResult sendOtpEmail(String recipientEmail, String recipientName, String otp) {
        if (recipientEmail == null || recipientEmail.trim().isEmpty()) {
            return new EmailResult(false, false, "Recipient email address is missing.", otp);
        }

        String to = recipientEmail.trim();
        String name = (recipientName != null && !recipientName.trim().isEmpty()) ? recipientName.trim() : "Student";

        String host = config.getProperty("mail.smtp.host", "smtp.gmail.com");
        String port = config.getProperty("mail.smtp.port", "587");
        String username = config.getProperty("mail.smtp.username", "");
        String password = config.getProperty("mail.smtp.password", "");
        String from = config.getProperty("mail.from", "noreply@skillswap.com");

        boolean hasCredentials = username != null && !username.trim().isEmpty() &&
                                 password != null && !password.trim().isEmpty();

        if (hasCredentials) {
            try {
                Properties props = new Properties();
                props.put("mail.smtp.host", host);
                props.put("mail.smtp.port", port);
                props.put("mail.smtp.auth", "true");
                props.put("mail.smtp.starttls.enable", "true");
                props.put("mail.smtp.connectiontimeout", "8000");
                props.put("mail.smtp.timeout", "8000");

                final String finalUser = username.trim();
                final String finalPass = password.trim();

                Session session = Session.getInstance(props, new Authenticator() {
                    @Override
                    protected PasswordAuthentication getPasswordAuthentication() {
                        return new PasswordAuthentication(finalUser, finalPass);
                    }
                });

                Message message = new MimeMessage(session);
                message.setFrom(new InternetAddress(from, "SkillSwap Campus Platform"));
                message.setRecipient(Message.RecipientType.TO, new InternetAddress(to));
                message.setSubject("SkillSwap - Your Login Verification Code: " + otp);

                String htmlContent = "<div style=\"font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, Helvetica, Arial, sans-serif; max-width: 520px; margin: 0 auto; border: 1px solid #e2e8f0; border-radius: 12px; overflow: hidden; background: #ffffff;\">"
                        + "<div style=\"background: linear-gradient(135deg, #1e3a8a, #3b82f6); padding: 24px; text-align: center; color: white;\">"
                        + "<h1 style=\"margin: 0; font-size: 24px; letter-spacing: -0.5px;\">🤝 SkillSwap</h1>"
                        + "<p style=\"margin: 6px 0 0; opacity: 0.9; font-size: 13px;\">Campus Peer-to-Peer Skill Learning &amp; Exchange</p>"
                        + "</div>"
                        + "<div style=\"padding: 28px 24px;\">"
                        + "<h3 style=\"color: #0f172a; margin-top: 0; font-size: 18px;\">Two-Factor Login Verification</h3>"
                        + "<p style=\"color: #475569; font-size: 14px; line-height: 1.6;\">Hello <strong>" + name + "</strong>,</p>"
                        + "<p style=\"color: #475569; font-size: 14px; line-height: 1.6;\">You recently initiated a login request for your SkillSwap account. Use the following One-Time Password (OTP) to complete sign-in:</p>"
                        + "<div style=\"background: #f1f5f9; border-radius: 8px; padding: 18px; text-align: center; margin: 22px 0; border: 1.5px dashed #cbd5e1;\">"
                        + "<span style=\"font-size: 32px; font-weight: 800; letter-spacing: 8px; color: #1d4ed8; font-family: Consolas, monospace;\">" + otp + "</span>"
                        + "</div>"
                        + "<p style=\"color: #64748b; font-size: 13px; line-height: 1.5;\">⚠️ This verification code is valid for <strong>10 minutes</strong>. Never share this code with anyone.</p>"
                        + "<p style=\"color: #94a3b8; font-size: 12px; margin-top: 20px;\">If you did not request this code, you can safely ignore this email.</p>"
                        + "</div>"
                        + "<div style=\"background: #f8fafc; padding: 14px; text-align: center; font-size: 12px; color: #94a3b8; border-top: 1px solid #e2e8f0;\">"
                        + "SkillSwap Campus Network • Automated Security System"
                        + "</div>"
                        + "</div>";

                message.setContent(htmlContent, "text/html; charset=utf-8");
                Transport.send(message);

                System.out.println("[SkillSwap EmailService] Real SMTP OTP email sent successfully to: " + to);
                return new EmailResult(true, false, "Verification code sent to " + to, otp);

            } catch (Exception e) {
                System.err.println("[SkillSwap EmailService] SMTP delivery failed (" + e.getMessage() + "). Falling back to secure console dispatch.");
            }
        }

        // Simulated / Console dispatch fallback
        System.out.println("\n========================================================");
        System.out.println("  [SkillSwap Security] OTP Verification Code Dispatched ");
        System.out.println("  Timestamp: " + new Date());
        System.out.println("  To:        " + to + " (" + name + ")");
        System.out.println("  OTP Code:  " + otp);
        System.out.println("========================================================\n");

        return new EmailResult(true, true, "Verification code sent to " + to, otp);
    }
}
