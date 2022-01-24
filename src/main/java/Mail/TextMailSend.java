package Mail;

import javax.mail.*;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;
import java.util.Date;
import java.util.Properties;

public class TextMailSend {

    //发件人地址
    public static String fromAddress = "gantian1106@163.com";
    //发件人密码,163邮箱授权码，并非登录密码
    public static String fromPassword = "VUIZMZOZSAJBVGOM";
    //收件人地址
    public static String toAddress = "504161389@qq.com";

    public static void main(String[] args) throws Exception {
        //1. 创建参数配置, 用于连接邮件服务器的参数配置
        Properties props = new Properties();
        //设置用户的认证方式
        props.setProperty("mail.smtp.auth","true");
        //设置传输协议
        props.setProperty("mail.smtp.protocol","smtp");
        //设置发件人的SMTP地址
        props.setProperty("mail.smtp.host","smtp.163.com");

        //2.根据配置创建会话对象, 用于和邮件服务器交互
        Session session = Session.getDefaultInstance(props);
        session.setDebug(true);

        //3.创建邮件的实例对象
        MimeMessage message = createTextMimeMessage(session);

        //4.根据session对象获取邮件传输对象Transport
        Transport transport = session.getTransport();
        //设置发件人的账户名和密码
        transport.connect(fromAddress,fromPassword);
        //发送邮件，并发送到所有收件人地址，message.getAllRecipients() 获取到的是在创建邮件对象时添加的所有收件人, 抄送人, 密送人
        transport.sendMessage(message,message.getAllRecipients());
        //5.关闭邮件连接
        transport.close();
    }

    /**
     * 创建一封纯文本邮件内容
     * @param session
     * @return
     */
    public static MimeMessage createTextMimeMessage(Session session) throws Exception{
        // 创建MimeMessage实例对象
        MimeMessage textMessage = new MimeMessage(session);
        //设置发件人
        textMessage.setFrom(new InternetAddress(fromAddress));
        //设置收件人
        textMessage.setRecipient(Message.RecipientType.TO,new InternetAddress(toAddress));
        //设置主题
        textMessage.setSubject("test");
        //设置正文
        textMessage.setText("您好,这是来自一封kiragan的测试邮件");
        //设置发送时间
        textMessage.setSentDate(new Date());
        //保存设置
        textMessage.saveChanges();
        return textMessage;
    }
}
