package com.yash.backend;

import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api")
public class DebugController {

    private final DebugService debugService;

    public DebugController(DebugService debugService) {
        this.debugService = debugService;
    }

    @PostMapping("/debug")
    public DebugResponse debug(@RequestBody DebugRequest request) {
        return debugService.analyze(request);
    }
}
