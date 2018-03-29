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

package com.migo.shiro;

import com.migo.entity.SysMenuEntity;
import com.migo.entity.SysUserEntity;
import com.migo.service.SysMenuService;
import com.migo.service.SysUserService;
import org.apache.commons.lang.StringUtils;
import org.apache.shiro.authc.*;
import org.apache.shiro.authz.AuthorizationInfo;
import org.apache.shiro.authz.SimpleAuthorizationInfo;
import org.apache.shiro.realm.AuthorizingRealm;
import org.apache.shiro.subject.PrincipalCollection;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.*;
import java.util.stream.Collectors;

/**
 * @author 知秋
 * @email fei6751803@163.com
 */
public class UserRealm extends AuthorizingRealm {
    @Autowired
    private SysUserService sysUserService;
    @Autowired
    private SysMenuService sysMenuService;

    /**
     * 授权(验证权限时调用)
     * 一系类的实体信息
     */
    @Override
    protected AuthorizationInfo doGetAuthorizationInfo(PrincipalCollection principalCollection) {
        SysUserEntity user = (SysUserEntity) principalCollection.getPrimaryPrincipal();
        Long userId = user.getUserId();

        // 权限列表
        List<String> permsList;

        //系统管理员，拥有最高权限
        if (userId == 1) {
            // 查询出所有的菜单SELECT * from sys_menu，包括按钮记录
            List<SysMenuEntity> menuList = sysMenuService.queryList(new HashMap<>());
            permsList = menuList.stream()   // java8新特性，获取流java.util.stream
                    .parallel()
                    .map(SysMenuEntity::getPerms)
                    .collect(Collectors.toList());
            /*permsList = new ArrayList<>(menuList.size());
            for(SysMenuEntity menu : menuList){
                permsList.add(menu.getPerms());
            }*/
        } else {
            // 该用户下的权限
            permsList = sysUserService.queryAllPerms(userId);
        }

        //用户权限列表
       /* Set<String> permsSet = new HashSet<>();
        for(String perms : permsList){
            if(StringUtils.isBlank(perms)){
                continue;
            }
            permsSet.addAll(Arrays.asList(perms.trim().split(",")));
        }*/
        Set<String> permsSet = permsList.stream().parallel()
                .filter(StringUtils::isNotBlank)
                .map(String::trim)
                .map(s -> s.split(","))
                .map(Arrays::asList)
                .flatMap(Collection::stream)
                .collect(Collectors.toSet());
        SimpleAuthorizationInfo info = new SimpleAuthorizationInfo();
        // 将权限放入认证信息里面
        info.setStringPermissions(permsSet);
        return info;
    }

    /**
     * 根据用户名和密码验证用户信息，自定义
     * @param authenticationToken
     * @return
     * @throws AuthenticationException
     */
    @Override
    protected AuthenticationInfo doGetAuthenticationInfo(AuthenticationToken authenticationToken) throws AuthenticationException {
        String username = (String) authenticationToken.getPrincipal();
        String password = new String((char[]) authenticationToken.getCredentials());

        //查询用户信息
        SysUserEntity user = sysUserService.queryByUserName(username);

        //账号不存在
        if (user == null) {
            throw new UnknownAccountException("账号或密码不正确");
        }

        //密码错误
        if (!password.equals(user.getPassword())) {
            throw new IncorrectCredentialsException("账号或密码不正确");
        }

        //账号锁定
        if (user.getStatus() == 0) {
            throw new LockedAccountException("账号已被锁定,请联系管理员");
        }

        return new SimpleAuthenticationInfo(user, password, getName());
    }

}
