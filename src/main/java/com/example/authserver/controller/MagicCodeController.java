package com.example.authserver.controller;

import com.example.authserver.service.MagicCodeService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/magic-code")
@RequiredArgsConstructor
public class MagicCodeController {

    private final MagicCodeService magicCodeService;

    @PostMapping("/issue")
    public ResponseEntity<Map<String, String>> issue(@RequestParam String username) {
        String code = magicCodeService.issue(username);
        // In production: send via email/SMS and return 204. Returning code here for dev/testing.
        return ResponseEntity.ok(Map.of("code", code));
    }
}
