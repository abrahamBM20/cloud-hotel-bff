package com.hotel.bff.health;

import com.hotel.bff.health.dto.HealthResponseDto;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api")
public class HealthCheckController {

    @GetMapping("/health")
    public ResponseEntity<HealthResponseDto> getHealth() {
        return ResponseEntity.ok(new HealthResponseDto("UP"));
    }
}
