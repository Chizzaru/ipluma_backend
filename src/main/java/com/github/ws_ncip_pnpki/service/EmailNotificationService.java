package com.github.ws_ncip_pnpki.service;

import com.github.ws_ncip_pnpki.model.User;
import com.github.ws_ncip_pnpki.repository.UserRepository;
import jakarta.mail.Message;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.InternetAddress;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

import jakarta.mail.internet.MimeMessage;

import java.io.UnsupportedEncodingException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Date;

import org.thymeleaf.spring6.SpringTemplateEngine;

@Service
public class EmailNotificationService {

    @Autowired
    private JavaMailSender mailSender;

    @Autowired
    private SpringTemplateEngine springTemplateEngine;

    @Value("${app.mail.from-address}")
    private String fromAddress;

    @Value("${app.mail.from-name}")
    private String fromName;

    @Value("${spring.mail.username}")
    private String fromEmail;

    @Value("${app.url:http://localhost:5173}")
    private String appUrl;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private TemporaryCredentialService temporaryCredentialService;


    // Try different formats
    public void sendEmailWithDifferentFormats(String to, String subject, String text) throws UnsupportedEncodingException, MessagingException {
        SimpleMailMessage message = new SimpleMailMessage();

        // Option 1: Plain name only (sometimes works better)
        message.setFrom("NCIP Applications <apps.notif@ncip.gov.ph>");

        // Option 2: With quotes
        message.setFrom("\"NCIP Applications\" <apps.notif@ncip.gov.ph>");

        // Option 3: Direct address only
        message.setFrom("apps.notif@ncip.gov.ph");

        // Option 4: Using MimeMessageHelper
        MimeMessage mimeMessage = mailSender.createMimeMessage();
        MimeMessageHelper helper = new MimeMessageHelper(mimeMessage);
        helper.setFrom(new InternetAddress("apps.notif@ncip.gov.ph", "NCIP Applications"));

        message.setTo(to);
        message.setSubject(subject);
        message.setText(text);

        mailSender.send(message);
    }

    /**
     * Send simple text email using your MAIL_FROM_NAME format
     */
    public void sendSimpleEmail(String to, String subject, String text) throws MessagingException, UnsupportedEncodingException {
        SimpleMailMessage message = new SimpleMailMessage();

        // For SimpleMailMessage, use string format
        String fromHeader = String.format("%s <%s>", fromName, fromAddress);
        message.setFrom(fromHeader);

        message.setTo(to);
        message.setSubject(subject);
        message.setText(text);

        mailSender.send(message);


    }

    /**
     * Send HTML email
     */
    public void sendHtmlEmail(Long userId)
            throws MessagingException, UnsupportedEncodingException {

        User user = userRepository.findById(userId).orElseThrow();

        String pass = temporaryCredentialService.getTemporaryPassword(user.getEmployee().getId());

        System.out.println(user.getEmployee().getFirstName());
        System.out.println(user.getUsername());
        System.out.println(pass);

        MimeMessage message = mailSender.createMimeMessage();
        // Set FROM directly on the message, not through helper
        InternetAddress from = new InternetAddress(fromAddress, fromName);
        message.setFrom(from);

        // Then use helper for everything else
        MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
        helper.setTo(user.getEmail());
        helper.setSubject("iPluma | Temporary Password");
        String emailContent = htmlContent(user.getEmployee().getFirstName(), user.getUsername(), pass);
        helper.setText(emailContent, true);

        mailSender.send(message);

        temporaryCredentialService.updateTempEmailSentToTrue(user.getEmployee().getId());


    }



    private String htmlContent(String recipientName, String username, String temporaryPassword){
        int currentYear = LocalDate.now().getYear();

        return """
<!DOCTYPE html>
<html lang="en">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>Temporary Password - iPluma</title>
    <style>
        * {
            margin: 0;
            padding: 0;
            box-sizing: border-box;
            font-family: 'Segoe UI', Tahoma, Geneva, Verdana, sans-serif;
        }
        body {
            background-color: #f5f7fa;
            margin: 0;
            padding: 20px;
        }
        
        .email-container {
            max-width: 600px;
            margin: 0 auto;
            background-color: #ffffff;
            border-radius: 12px;
            overflow: hidden;
            box-shadow: 0 4px 20px rgba(0, 0, 0, 0.08);
        }
        
        .header {
            background: linear-gradient(135deg, #007bff 0%%, #0056b3 100%%);
            color: white;
            padding: 30px 40px;
            text-align: center;
        }
        
        .logo {
            font-size: 28px;
            font-weight: 700;
            margin-bottom: 10px;
            display: flex;
            align-items: center;
            justify-content: center;
            gap: 10px;
        }
        
        .logo-icon {
            font-size: 32px;
        }
        
        .header h1 {
            font-size: 24px;
            font-weight: 600;
            margin: 0;
        }
        
        .content {
            padding: 40px;
            color: #333333;
            line-height: 1.6;
        }
        
        .greeting {
            font-size: 18px;
            margin-bottom: 25px;
            color: #2c3e50;
        }
        
        .greeting strong {
            color: #007bff;
        }
        
        .password-box {
            background: linear-gradient(135deg, #f8f9fa 0%%, #e9ecef 100%%);
            border: 2px dashed #007bff;
            border-radius: 8px;
            padding: 25px;
            margin: 25px 0;
            text-align: center;
        }
        
        .password-label {
            font-size: 14px;
            color: #6c757d;
            margin-bottom: 10px;
            text-transform: uppercase;
            letter-spacing: 1px;
        }
        
        .password {
            font-family: 'Courier New', monospace;
            font-size: 28px;
            font-weight: 700;
            color: #e74c3c;
            letter-spacing: 2px;
            padding: 10px;
            background-color: white;
            border-radius: 6px;
            margin: 15px 0;
            user-select: all;
            box-shadow: 0 2px 5px rgba(0, 0, 0, 0.1);
        }
        
        .warning-box {
            background-color: #fff3cd;
            border: 1px solid #ffc107;
            border-left: 5px solid #ffc107;
            border-radius: 6px;
            padding: 20px;
            margin: 25px 0;
        }
        
        .warning-title {
            color: #856404;
            font-weight: 600;
            margin-bottom: 10px;
            display: flex;
            align-items: center;
            gap: 10px;
        }
        
        .instructions {
            background-color: #f8f9fa;
            border-radius: 8px;
            padding: 25px;
            margin: 25px 0;
        }
        
        .instructions ol {
            margin-left: 20px;
            margin-top: 10px;
        }
        
        .instructions li {
            margin-bottom: 12px;
        }
        
        .expiry-notice {
            text-align: center;
            padding: 15px;
            background-color: #e8f4ff;
            border-radius: 8px;
            margin: 25px 0;
            font-weight: 600;
            color: #0056b3;
        }
        
        .button-container {
            text-align: center;
            margin: 30px 0;
        }
        
        .login-button {
            display: inline-block;
            background: linear-gradient(135deg, #007bff 0%%, #0056b3 100%%);
            color: white;
            text-decoration: none;
            padding: 15px 40px;
            border-radius: 50px;
            font-weight: 600;
            font-size: 16px;
            transition: all 0.3s ease;
            box-shadow: 0 4px 15px rgba(0, 123, 255, 0.3);
        }
        
        .login-button:hover {
            transform: translateY(-2px);
            box-shadow: 0 6px 20px rgba(0, 123, 255, 0.4);
        }
        
        .footer {
            background-color: #f8f9fa;
            padding: 25px 40px;
            text-align: center;
            color: #6c757d;
            font-size: 14px;
            border-top: 1px solid #e9ecef;
        }
        
        .footer-links {
            margin-top: 15px;
        }
        
        .footer-links a {
            color: #007bff;
            text-decoration: none;
            margin: 0 10px;
        }
        
        .security-tip {
            background-color: #d1ecf1;
            border: 1px solid #bee5eb;
            border-radius: 6px;
            padding: 15px;
            margin-top: 20px;
            font-size: 13px;
            color: #0c5460;
        }
        
        @media (max-width: 600px) {
            .content, .header {
                padding: 25px 20px;
            }
            
            .password {
                font-size: 22px;
            }
        }
    </style>
</head>
<body>
    <div class="email-container">
        <!-- Header -->
        <div class="header">
            <div class="logo">
                <span class="logo-icon">🔐</span>
                iPluma Security
            </div>
            <h1 style="color:black">Temporary Password Notification</h1>
        </div>
        
        <!-- Content -->
        <div class="content">
            <!-- Greeting -->
            <div class="greeting">
                Hello <strong>%s</strong>,
            </div>
            
            <!-- Main Message -->
            <p>A temporary password has been generated for your iPluma account. Below are your login credentials:</p>
            
            <!-- Username -->
            <div style="margin: 20px 0; padding: 15px; background-color: #f8f9fa; border-radius: 6px;">
                <div style="font-size: 14px; color: #6c757d; margin-bottom: 5px;">Username</div>
                <div style="font-size: 18px; font-weight: 600; color: #2c3e50;">%s</div>
            </div>
            
            <!-- Temporary Password -->
            <div class="password-box">
                <div class="password-label">Your Temporary Password</div>
                <div class="password">%s</div>
                <div style="font-size: 14px; color: #6c757d; margin-top: 10px;">
                    Click to copy or select the password above
                </div>
            </div>
            
            
            
            <!-- Login Button -->
            <div class="button-container">
                <a href="https://ipluma.ncip.gov.ph/login" class="login-button" style="color:black;">
                    Login to iPluma Now
                </a>
            </div>
            
            <!-- Security Tips -->
            <div class="security-tip">
                <strong>💡 Security Tip:</strong> Never share your password with anyone. iPluma staff will never ask for your password via email, phone, or chat.
            </div>
        </div>
      
        <!-- Footer -->
        <div class="footer">
            <p>This email was sent by <strong>iPluma Security System</strong></p>
            
            <p style="margin-top: 20px; font-size: 12px; color: #adb5bd;">
                © %d iPluma System. National Commission on Indigenous Peoples.<br>
                This is an automated message, please do not reply to this email.
            </p>
        </div>
    </div>
</body>
</html>
""".formatted(recipientName, username, temporaryPassword, currentYear);
    }


    /**
     * Alternative: Using InternetAddress directly
     */
    public void sendHtmlEmailAlternative(String to, String subject, String htmlContent)
            throws MessagingException, UnsupportedEncodingException {
        MimeMessage message = mailSender.createMimeMessage();
        MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

        // Create InternetAddress with display name
        InternetAddress from = new InternetAddress(fromAddress, fromName);
        helper.setFrom(from);

        helper.setTo(to);
        helper.setSubject(subject);
        helper.setText(htmlContent, true);

        mailSender.send(message);
    }


    /**
     * Send email with attachment
     */
    public void sendEmailWithAttachment(String to, String subject, String text,
                                        String attachmentName, Resource resource)
            throws MessagingException {
        MimeMessage message = mailSender.createMimeMessage();
        MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

        String fromHeader = String.format("%s <%s>", fromName, fromAddress);
        helper.setFrom(fromHeader);
        helper.setTo(to);
        helper.setSubject(subject);
        helper.setText(text);

        helper.addAttachment(attachmentName, resource);

        mailSender.send(message);
        System.out.println("Email with attachment sent to: " + to);
    }

    /**
     * Send email to multiple recipients
     */
    public void sendBulkEmail(String[] to, String subject, String text) {
        SimpleMailMessage message = new SimpleMailMessage();
        String fromHeader = String.format("%s <%s>", fromName, fromAddress);
        message.setFrom(fromHeader);
        message.setTo(to);
        message.setSubject(subject);
        message.setText(text);

        mailSender.send(message);
        System.out.println("Bulk email sent to " + to.length + " recipients");
    }

    /**
     * Send batch signing completion notification
     */
    public void sendBatchSigningNotification(
            String toEmail,
            String batchId,
            int totalDocuments,
            int successCount,
            int failureCount) {

        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            helper.setFrom(fromEmail);
            helper.setTo(toEmail);
            helper.setSubject("PDF Batch Signing Complete - Batch ID: " + batchId);

            String htmlContent = buildBatchSigningEmail(batchId, totalDocuments, successCount, failureCount);
            helper.setText(htmlContent, true);

            mailSender.send(message);

            System.out.println("✓ Email notification sent to: " + toEmail);

        } catch (Exception e) {
            System.err.println("✗ Failed to send email: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Send single document signing notification
     */
    public void sendSigningNotification(
            String toEmail,
            String originalFilename,
            String signedFilename,
            boolean hasTimestamp) {

        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            helper.setFrom(fromEmail);
            helper.setTo(toEmail);
            helper.setSubject("PDF Document Signed: " + originalFilename);

            String htmlContent = buildSigningEmail(originalFilename, signedFilename, hasTimestamp);
            helper.setText(htmlContent, true);

            mailSender.send(message);

            System.out.println("✓ Email notification sent to: " + toEmail);

        } catch (Exception e) {
            System.err.println("✗ Failed to send email: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Send verification result notification
     */
    public void sendVerificationNotification(
            String toEmail,
            String filename,
            boolean allValid,
            int signatureCount) {

        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            helper.setFrom(fromEmail);
            helper.setTo(toEmail);
            helper.setSubject("PDF Verification Result: " + filename);

            String htmlContent = buildVerificationEmail(filename, allValid, signatureCount);
            helper.setText(htmlContent, true);

            mailSender.send(message);

            System.out.println("✓ Email notification sent to: " + toEmail);

        } catch (Exception e) {
            System.err.println("✗ Failed to send email: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private String buildBatchSigningEmail(String batchId, int total, int success, int failure) {
        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));

        return String.format("""
                <!DOCTYPE html>
                <html>
                <head>
                    <style>
                        body { font-family: Arial, sans-serif; line-height: 1.6; color: #333; }
                        .container { max-width: 600px; margin: 0 auto; padding: 20px; }
                        .header { background: #4CAF50; color: white; padding: 20px; text-align: center; border-radius: 5px 5px 0 0; }
                        .content { background: #f9f9f9; padding: 20px; border: 1px solid #ddd; border-radius: 0 0 5px 5px; }
                        .stats { display: flex; justify-content: space-around; margin: 20px 0; }
                        .stat { text-align: center; padding: 15px; background: white; border-radius: 5px; flex: 1; margin: 0 5px; }
                        .stat-number { font-size: 32px; font-weight: bold; color: #4CAF50; }
                        .failure { color: #f44336; }
                        .footer { margin-top: 20px; padding-top: 20px; border-top: 1px solid #ddd; font-size: 12px; color: #666; }
                        .button { display: inline-block; padding: 10px 20px; background: #4CAF50; color: white; text-decoration: none; border-radius: 5px; margin-top: 10px; }
                    </style>
                </head>
                <body>
                    <div class="container">
                        <div class="header">
                            <h1>📄 Batch Signing Complete</h1>
                        </div>
                        <div class="content">
                            <p>Hello,</p>
                            <p>Your batch signing operation has been completed.</p>
                            
                            <div style="background: #e3f2fd; padding: 15px; border-radius: 5px; margin: 15px 0;">
                                <strong>Batch ID:</strong> %s<br>
                                <strong>Completed:</strong> %s
                            </div>
                            
                            <div class="stats">
                                <div class="stat">
                                    <div class="stat-number">%d</div>
                                    <div>Total Documents</div>
                                </div>
                                <div class="stat">
                                    <div class="stat-number">%d</div>
                                    <div>✓ Success</div>
                                </div>
                                <div class="stat">
                                    <div class="stat-number failure">%d</div>
                                    <div>✗ Failed</div>
                                </div>
                            </div>
                            
                            <p>You can download your signed documents from the application.</p>
                            
                            <a href="%s" class="button">View Signed Documents</a>
                            
                            <div class="footer">
                                <p>This is an automated notification from the PDF Signing Service.</p>
                                <p>If you did not request this action, please contact support immediately.</p>
                            </div>
                        </div>
                    </div>
                </body>
                </html>
                """, batchId, timestamp, total, success, failure, appUrl);
    }

    private String buildSigningEmail(String original, String signed, boolean hasTimestamp) {
        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
        String tsaStatus = hasTimestamp ? "✓ Included" : "✗ Not included";
        String tsaColor = hasTimestamp ? "#4CAF50" : "#ff9800";

        return String.format("""
                <!DOCTYPE html>
                <html>
                <head>
                    <style>
                        body { font-family: Arial, sans-serif; line-height: 1.6; color: #333; }
                        .container { max-width: 600px; margin: 0 auto; padding: 20px; }
                        .header { background: #2196F3; color: white; padding: 20px; text-align: center; border-radius: 5px 5px 0 0; }
                        .content { background: #f9f9f9; padding: 20px; border: 1px solid #ddd; border-radius: 0 0 5px 5px; }
                        .info-box { background: white; padding: 15px; border-left: 4px solid #2196F3; margin: 15px 0; }
                        .footer { margin-top: 20px; padding-top: 20px; border-top: 1px solid #ddd; font-size: 12px; color: #666; }
                    </style>
                </head>
                <body>
                    <div class="container">
                        <div class="header">
                            <h1>✅ Document Signed Successfully</h1>
                        </div>
                        <div class="content">
                            <p>Hello,</p>
                            <p>Your PDF document has been digitally signed.</p>
                            
                            <div class="info-box">
                                <strong>Original File:</strong> %s<br>
                                <strong>Signed File:</strong> %s<br>
                                <strong>Signed At:</strong> %s<br>
                                <strong>TSA Timestamp:</strong> <span style="color: %s;">%s</span>
                            </div>
                            
                            <p>The document is now ready for download and can be verified at any time.</p>
                            
                            <div class="footer">
                                <p>This is an automated notification from the PDF Signing Service.</p>
                            </div>
                        </div>
                    </div>
                </body>
                </html>
                """, original, signed, timestamp, tsaColor, tsaStatus);
    }

    private String buildVerificationEmail(String filename, boolean valid, int count) {
        String status = valid ? "✓ All Signatures Valid" : "⚠ Issues Found";
        String color = valid ? "#4CAF50" : "#f44336";

        return String.format("""
                <!DOCTYPE html>
                <html>
                <head>
                    <style>
                        body { font-family: Arial, sans-serif; line-height: 1.6; color: #333; }
                        .container { max-width: 600px; margin: 0 auto; padding: 20px; }
                        .header { background: %s; color: white; padding: 20px; text-align: center; border-radius: 5px 5px 0 0; }
                        .content { background: #f9f9f9; padding: 20px; border: 1px solid #ddd; border-radius: 0 0 5px 5px; }
                        .status-box { background: white; padding: 20px; text-align: center; margin: 20px 0; border-radius: 5px; }
                        .status-text { font-size: 24px; font-weight: bold; color: %s; }
                        .footer { margin-top: 20px; padding-top: 20px; border-top: 1px solid #ddd; font-size: 12px; color: #666; }
                    </style>
                </head>
                <body>
                    <div class="container">
                        <div class="header">
                            <h1>🔍 Verification Complete</h1>
                        </div>
                        <div class="content">
                            <p>Hello,</p>
                            <p>Verification has been completed for your PDF document.</p>
                            
                            <div class="status-box">
                                <div><strong>File:</strong> %s</div>
                                <div style="margin: 15px 0;">
                                    <div class="status-text">%s</div>
                                </div>
                                <div><strong>Signatures Found:</strong> %d</div>
                            </div>
                            
                            <p>For detailed verification results, please check the application.</p>
                            
                            <div class="footer">
                                <p>This is an automated notification from the PDF Signing Service.</p>
                            </div>
                        </div>
                    </div>
                </body>
                </html>
                """, color, color, filename, status, count);
    }
}