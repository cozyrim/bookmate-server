package com.exercise.bookmateserver.discovery;

import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/discovery/bestsellers")
public class BestsellerController {
    private final BestsellerService service;
    public BestsellerController(BestsellerService service) { this.service = service; }
    @GetMapping("/realtime")
    public BestsellerResponse realtime() { return service.realtime(); }
}
