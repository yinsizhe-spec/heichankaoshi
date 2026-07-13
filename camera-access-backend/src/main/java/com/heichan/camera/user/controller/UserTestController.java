package com.heichan.camera.user.controller;

import com.heichan.camera.user.entity.AppUser;
import com.heichan.camera.user.mapper.AppUserMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/test/users")
@RequiredArgsConstructor
public class UserTestController {

    private final AppUserMapper appUserMapper;

    @GetMapping
    public List<AppUser> listUsers() {
        return appUserMapper.selectList(null);
    }
}