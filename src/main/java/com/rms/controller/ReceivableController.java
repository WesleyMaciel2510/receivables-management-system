package com.rms.controller;

import com.rms.dto.ReceivableRequest;
import com.rms.dto.ReceivableResponse;
import com.rms.dto.StatusChangeRequest;
import com.rms.service.ReceivableService;
import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.util.UriComponentsBuilder;
import io.swagger.v3.oas.annotations.tags.Tag;

@RestController
@RequestMapping("/receivables")
@Tag(name = "Receivables", description = "Cadastro, consulta e ciclo de vida dos recebÃƒÂ­veis")
public class ReceivableController {
    private final ReceivableService service;

    public ReceivableController(ReceivableService service) { this.service = service; }

    @PostMapping
    public ResponseEntity<ReceivableResponse> create(@Valid @RequestBody ReceivableRequest request, UriComponentsBuilder uri) {
        ReceivableResponse response = service.create(request);
        return ResponseEntity.created(uri.path("/receivables/{id}").build(response.id())).body(response);
    }

    @GetMapping
    public List<ReceivableResponse> list() { return service.list(); }

    @GetMapping("/archived")
    public List<ReceivableResponse> archived() { return service.listArchived(); }

    @GetMapping("/{id}")
    public ReceivableResponse get(@PathVariable UUID id) { return service.get(id); }

    @PutMapping("/{id}")
    public ReceivableResponse update(@PathVariable UUID id, @Valid @RequestBody ReceivableRequest request) {
        return service.update(id, request);
    }

    @PatchMapping("/{id}/status")
    public ReceivableResponse changeStatus(@PathVariable UUID id, @Valid @RequestBody StatusChangeRequest request) {
        return service.changeStatus(id, request.status());
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable UUID id) {
        service.delete(id);
        return ResponseEntity.noContent().build();
    }
}
