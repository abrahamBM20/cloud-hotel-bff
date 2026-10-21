package com.hotel.bff.rooms;

import com.hotel.bff.rooms.dto.RoomDto;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

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
}
