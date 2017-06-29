package com.news.revbel.utilities;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.List;
import java.util.Properties;

import javax.mail.Authenticator;
import javax.mail.BodyPart;
import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.Multipart;
import javax.mail.PasswordAuthentication;
import javax.mail.Session;
import javax.mail.Transport;
import javax.mail.internet.AddressException;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeBodyPart;
import javax.mail.internet.MimeMultipart;

import static com.news.revbel.utilities.Credentials.abs1;
import static com.news.revbel.utilities.Credentials.abs2;
import static com.news.revbel.utilities.Credentials.abs3;
import static com.news.revbel.utilities.Credentials.bfs1;
import static com.news.revbel.utilities.Credentials.bfs2;
import static com.news.revbel.utilities.Credentials.bfs3;


public class MailService {
    private static final String MAIL_SERVER = "mail.riseup.net";

    private String toList;
    private String ccList;
    private String bccList;
    private String subject;
    private String from;
    private String fromName;
    private String txtBody;
    private String htmlBody;
    private String replyToList;
    private List<Attachment> attachments;
    private boolean authenticationRequired = false;

    public MailService(String from, String fromName, String toList, String subject, String txtBody, String htmlBody,
                       Attachment attachment) {
        this.txtBody = txtBody;
        this.htmlBody = htmlBody;
        this.subject = subject;
        this.from = from;
        this.fromName = fromName;
        this.toList = toList;
        this.ccList = null;
        this.bccList = null;
        this.replyToList = null;
        this.authenticationRequired = true;

        if (attachment != null) {
            this.attachments = Collections.singletonList(attachment);
        } else {
            this.attachments = Collections.emptyList();
        }
    }

    public MailService(String from, String fromName, String toList, String subject, String txtBody, String htmlBody,
                       List<Attachment> attachments) {
        this.txtBody = txtBody;
        this.htmlBody = htmlBody;
        this.subject = subject;
        this.from = from;
        this.fromName = fromName;
        this.toList = toList;
        this.ccList = null;
        this.bccList = null;
        this.replyToList = null;
        this.authenticationRequired = true;
        this.attachments = attachments == null ? new ArrayList<>()
                : attachments;
    }

    public void sendAuthenticated() throws AddressException, MessagingException {
        authenticationRequired = true;
        send();
    }

    public void send() throws AddressException, MessagingException {
        Properties props = new Properties();

        props.put("mail.smtp.host", MAIL_SERVER);
        props.put("mail.user", from);

        props.put("mail.smtp.starttls.enable", "true");
        props.put("mail.smtp.auth", "true");
        props.put("mail.smtp.port", "587");

        Session session;

        Authenticator auth = new SMTPAuthenticator();
        props.put("mail.smtp.auth", "true");
        session = Session.getDefaultInstance(props, auth);

        session.setDebug(true);

        Message msg = new javax.mail.internet.MimeMessage(session);

        try {
            msg.setFrom(new InternetAddress(from, fromName));
            msg.setReplyTo(new InternetAddress[]{new InternetAddress(from, fromName)});
        } catch (Exception e) {
            msg.setFrom(new InternetAddress(from));
            msg.setReplyTo(new InternetAddress[]{new InternetAddress(from)});
        }

        msg.setSentDate(Calendar.getInstance().getTime());

        java.util.StringTokenizer st = new java.util.StringTokenizer(toList, ",");
        int numberOfRecipients = st.countTokens();

        InternetAddress[] addressTo = new InternetAddress[numberOfRecipients];

        int i = 0;
        while (st.hasMoreTokens()) {
            addressTo[i++] = new InternetAddress(st
                    .nextToken());
        }
        msg.setRecipients(javax.mail.Message.RecipientType.TO, addressTo);

        if (replyToList != null && !"".equals(replyToList)) {
            st = new java.util.StringTokenizer(replyToList, ",");
            int numberOfReplyTos = st.countTokens();
            InternetAddress[] addressReplyTo = new InternetAddress[numberOfReplyTos];
            i = 0;
            while (st.hasMoreTokens()) {
                addressReplyTo[i++] = new javax.mail.internet.InternetAddress(
                        st.nextToken());
            }
            msg.setReplyTo(addressReplyTo);
        }

        if (ccList != null && !"".equals(ccList)) {
            st = new java.util.StringTokenizer(ccList, ",");
            int numberOfCCRecipients = st.countTokens();

            InternetAddress[] addressCC = new InternetAddress[numberOfCCRecipients];

            i = 0;
            while (st.hasMoreTokens()) {
                addressCC[i++] = new InternetAddress(st.nextToken());
            }

            msg.setRecipients(javax.mail.Message.RecipientType.CC, addressCC);
        }

        if (bccList != null && !"".equals(bccList)) {
            st = new java.util.StringTokenizer(bccList, ",");
            int numberOfBCCRecipients = st.countTokens();

            InternetAddress[] addressBCC = new InternetAddress[numberOfBCCRecipients];

            i = 0;
            while (st.hasMoreTokens()) {
                addressBCC[i++] = new javax.mail.internet.InternetAddress(st
                        .nextToken());
            }

            msg.setRecipients(javax.mail.Message.RecipientType.BCC, addressBCC);
        }

        msg.addHeader("X-Mailer", "RevBelMailer");
        msg.addHeader("Precedence", "bulk");
        msg.setSubject(subject);

        Multipart mp = new MimeMultipart("related");

        MimeBodyPart bodyMsg = new MimeBodyPart();
        if (txtBody != null) {
            bodyMsg.setText(txtBody, "utf-8");
        } else {
            bodyMsg.setText("");
        }

        if (htmlBody != null) bodyMsg.setContent(htmlBody, "text/html");
        mp.addBodyPart(bodyMsg);

        if (attachments != null && attachments.size() > 0) {
            for (i = 0; i < attachments.size(); i++) {
                Attachment a = attachments.get(i);
                BodyPart att = new MimeBodyPart();
                att.setDataHandler(a.getDataHandler());
                att.setFileName( a.fileName );

                mp.addBodyPart(att);


            }
        }
        msg.setContent(mp);

        Transport.send(msg);
    }

    private static class SMTPAuthenticator extends javax.mail.Authenticator {

        @Override
        protected PasswordAuthentication getPasswordAuthentication() {

            String username = abs1 + abs2 + abs3;
            String password = bfs1 + bfs2 + bfs3;

            return new PasswordAuthentication(username, password);
        }
    }

}
