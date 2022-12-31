package com.learnkafka.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.learnkafka.entity.LibraryEvent;
import com.learnkafka.repository.LibraryEventRepository;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.RecoverableDataAccessException;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
@Slf4j
public class LibraryEventService {

    @Autowired
    private LibraryEventRepository libraryEventRepository;

    @Autowired
    private ObjectMapper objectMapper;

    public void processLibraryEvent(ConsumerRecord<Integer, String> consumerRecord) throws JsonProcessingException {

        LibraryEvent libraryEvent = objectMapper.readValue(consumerRecord.value(), LibraryEvent.class);

        log.info("Library event : {}", libraryEvent);

        if(libraryEvent != null && (libraryEvent.getLibraryEventId() != null && libraryEvent.getLibraryEventId() == 999)) {
            throw new RecoverableDataAccessException("Temporary Network Issue");
        }

        switch (libraryEvent.getLibraryEventType()) {
            case NEW -> save(libraryEvent);

            case UPDATE -> {
                validateLibraryEvent(libraryEvent);
                save(libraryEvent);
            }

            default -> log.info("Invalid Library Event Type");
        }

    }

    private void save(LibraryEvent libraryEvent) {
        libraryEvent.getBook().setLibraryEvent(libraryEvent);
        libraryEventRepository.save(libraryEvent);

        log.info("Successfully Persisted the library Event {} ", libraryEvent);
    }

    private void validateLibraryEvent(LibraryEvent libraryEvent) {
        if (libraryEvent.getLibraryEventId() == null) {
            throw new IllegalArgumentException("Library Event Id is missing");
        }

        Optional<LibraryEvent> libraryEventOptional = libraryEventRepository.findById(libraryEvent.getLibraryEventId());

        if (!libraryEventOptional.isPresent()) {
            throw new IllegalArgumentException("Not a valid library event");
        }

        log.info("Validation is successful for the library event : {} ", libraryEvent);
    }

}
