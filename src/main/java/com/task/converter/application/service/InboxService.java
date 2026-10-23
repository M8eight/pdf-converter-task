package com.task.converter.application.service;

import com.task.converter.application.dto.RequestedFileEvent;
import com.task.converter.infrastructure.persistence.repository.InboxRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class InboxService {

    private final InboxRepository inboxRepository;

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public int getInserted(RequestedFileEvent event) {
        return inboxRepository.insertIfAbsent(event.eventId());
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void remove(UUID eventId) {
        inboxRepository.deleteById(eventId);
    }

}
