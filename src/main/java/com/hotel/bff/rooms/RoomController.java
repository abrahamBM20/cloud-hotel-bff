package com.hotel.bff.rooms;

import com.hotel.bff.rooms.dto.RoomDto;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.http.HttpStatus;
import com.hotel.bff.rooms.dto.RoomRequestDto;
import jakarta.validation.Valid;

import java.util.List;

@RestController
@RequestMapping("/api/v1/rooms")
public class RoomController {

    private final RoomService roomService;

    public RoomController(RoomService roomService) {
        this.roomService = roomService;
    }

    /** Catálogo público de habitaciones. */
    @GetMapping
    public List<RoomDto> getAll() {
        return roomService.getAll();
    }

    @GetMapping("/{id}")
    public ResponseEntity<RoomDto> getById(@PathVariable String id) {
        return roomService.getById(id)
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<RoomDto> create(@Valid @RequestBody RoomRequestDto request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(roomService.create(request));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public RoomDto update(@PathVariable String id, @Valid @RequestBody RoomRequestDto request) {
        return roomService.update(id, request);
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> delete(@PathVariable String id) {
        roomService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
