/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.migo.controller;

import com.google.code.kaptcha.Constants;
import com.google.code.kaptcha.Producer;
import com.migo.utils.R;
import com.migo.utils.ShiroUtils;
import org.apache.shiro.authc.*;
import org.apache.shiro.crypto.hash.Sha256Hash;
import org.apache.shiro.session.Session;
import org.apache.shiro.session.mgt.eis.SessionDAO;
import org.apache.shiro.subject.Subject;
import org.apache.shiro.subject.support.DefaultSubjectContext;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.imageio.ImageIO;
import javax.servlet.ServletException;
import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServletResponse;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.util.Collection;

/**
 * 登录相关
 *
 * @author 知秋
 * @email fei6751803@163.com
 */
@RestController
public class SysLoginController extends AbstractController {
    @Autowired
    private Producer producer;

    @Autowired
    private SessionDAO sessionDAO;

    @RequestMapping("captcha.jpg")
    public void captcha(HttpServletResponse response)throws ServletException, IOException {
        response.setHeader("Cache-Control", "no-store, no-cache");
        response.setContentType("image/jpeg");

        //生成文字验证码
        String text = producer.createText();
        //生成图片验证码
        BufferedImage image = producer.createImage(text);
        //保存到shiro session
        ShiroUtils.setSessionAttribute(Constants.KAPTCHA_SESSION_KEY, text);

        ServletOutputStream out = response.getOutputStream();
        ImageIO.write(image, "jpg", out);
    }

    /**
     * 登录
     */
    @PostMapping("/sys/login")
    public R login(String username, String password, String captcha)throws IOException {

        Collection<Session> sessions = sessionDAO.getActiveSessions();
        for (Session session : sessions) {
            System.out.println("登录用户" + session.getAttribute(DefaultSubjectContext.PRINCIPALS_SESSION_KEY));
            if (session.getAttribute(DefaultSubjectContext.PRINCIPALS_SESSION_KEY) != null) {
                return R.error(session.getAttribute(DefaultSubjectContext.PRINCIPALS_SESSION_KEY)+"该用户已登录");
            }
        }

        String kaptcha = ShiroUtils.getKaptcha(Constants.KAPTCHA_SESSION_KEY);
        if(!captcha.equalsIgnoreCase(kaptcha)){
            return R.error("验证码不正确");
        }

        try{
            Subject subject = ShiroUtils.getSubject();
            //sha256加密
            password = new Sha256Hash(password).toHex();
            // 登陆令牌建为用户名和用户加密后的密码
            UsernamePasswordToken token = new UsernamePasswordToken(username, password);
            subject.login(token);
        }catch (UnknownAccountException | LockedAccountException | IncorrectCredentialsException e) {
            return R.error(e.getMessage());
        } catch (AuthenticationException e) {
            return R.error("账户验证失败");
        }

        return R.ok();
    }

    /**
     * 退出
     */
    @GetMapping("logout")
    public void logout(HttpServletResponse response) throws IOException {
        ShiroUtils.logout();
        response.sendRedirect("login.html");
    }
}
