package com.tao.graduate.thread;
import cn.hutool.core.codec.Base64;
import com.alibaba.fastjson.JSONObject;
import com.tao.graduate.algorithm.service.AlgorithmResultDealer;

import com.tao.graduate.tool.*;
import lombok.SneakyThrows;
import org.aspectj.weaver.patterns.TypePatternQuestions;
import org.bytedeco.javacv.FFmpegFrameGrabber;
import org.bytedeco.javacv.Frame;
import javax.annotation.Resource;
import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.*;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;
public class JavaSocket extends Thread{
    // 控制线程开启和结束的开关
    private boolean threadSwitch = true;
    private String cameraUrl;
    //FFmpegFrameGrabber可以理解为解码器，也可以理解为帧收集器
    private FFmpegFrameGrabber fFmpegFrameGrabber = null;
    public JavaSocket(String cameraIp, int cameraPort, String userName, String password, String cameraNumber) {
        this.cameraUrl = "rtsp://" + userName + ":" + password + "@" + cameraIp + ":" + String.valueOf(cameraPort);
    }
    @SneakyThrows
    @Override
    public void run() {
        System.out.print(" - - [Java] cameraUrl:" + cameraUrl);
        // 工具类中初始化 设置传输方式
        System.out.println("start");
        String base64;
        String content = "javaTest";
        // 访问服务进程的套接字
        Socket socket = null;
        List<TypePatternQuestions.Question> questions = new ArrayList<>();
        System.out.println("调用远程接口:host=>" + "localhost,port=>+PORT 12345");
        FFmpegFrameGrabber fFmpegFrameGrabber = FFmpegFrameTool.createFFmpegFrameGrabber(cameraUrl);
        FFmpegFrameTool.startFrameGrabber(fFmpegFrameGrabber);
        while (threadSwitch) {
            try {
                long startTime = System.currentTimeMillis();
                // 初始化套接字，设置访问服务的主机和进程端口号，HOST是访问python进程的主机名称，可以是IP地址或者域名，PORT是python进程绑定的端口号
                socket = new Socket("127.0.1.1", 12365);
                // 获取输出流对象
                OutputStream os = socket.getOutputStream();
                PrintStream out = new PrintStream(os);
                // 发送内容
                Frame frame = FFmpegFrameTool.getFrame(fFmpegFrameGrabber);
                if (frame == null) {
                    fFmpegFrameGrabber.stop();
                    FFmpegFrameTool.startFrameGrabber(fFmpegFrameGrabber);
                    System.out.println(" - - [ERROR] empty frame");
                    continue;
                }
                BufferedImage originalImage = FFmpegFrameTool.converter.convert(frame);
                //输出流
                ByteArrayOutputStream stream = new ByteArrayOutputStream();
                ImageIO.write(originalImage, "png", stream);
                base64 = Base64.encode(stream.toByteArray());
                JSONObject jsonObject = new JSONObject();
                jsonObject.put("content", base64);
                String str = jsonObject.toJSONString();
                out.print(str);
                // 告诉服务进程，内容发送完毕，可以开始处理
                out.print("over");
                // 获取服务进程的输入流
                InputStream is = socket.getInputStream();
                BufferedReader br = new BufferedReader(new InputStreamReader(is, "utf-8"));
                String tmp = null;
                StringBuilder sb = new StringBuilder();
                // 读取内容
                while ((tmp = br.readLine()) != null && threadSwitch)
                    sb.append(tmp).append('\n');
                // 解析结果
                long endTime = System.currentTimeMillis(); //获取结束时间
                System.out.println("程序运行时间： " + (endTime - startTime) + "ms");
                String res = "" + sb;
                //处理res结果
            } catch (IOException e) {
                e.printStackTrace();
            } finally {
                try {
                    if (socket != null) socket.close();
                } catch (IOException e) {
                }
                System.out.println(("远程接口调用结束."));
            }
        }
    }
}

