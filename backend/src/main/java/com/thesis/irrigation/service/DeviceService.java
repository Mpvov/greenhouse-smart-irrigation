package com.thesis.irrigation.service;

import com.thesis.irrigation.domain.model.Device;
import com.thesis.irrigation.domain.repository.DeviceRepository;
import com.thesis.irrigation.domain.repository.GreenhouseRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Service
@RequiredArgsConstructor
@Slf4j
public class DeviceService {

    private final DeviceRepository deviceRepository;
    private final GreenhouseRepository greenhouseRepository;

    public Flux<Device> getByGreenhouse(String greenhouseId, String ownerId) {
        return greenhouseRepository.findById(greenhouseId)
                .filter(gh -> gh.ownerId().equals(ownerId))
                .flatMapMany(gh -> deviceRepository.findByGreenhouseId(greenhouseId));
    }

    public Mono<Device> createDevice(Device device, String ownerId) {
        return greenhouseRepository.findById(device.greenhouseId())
                .filter(gh -> gh.ownerId().equals(ownerId))
                .flatMap(gh -> deviceRepository.save(device))
                .switchIfEmpty(Mono.error(new RuntimeException("Greenhouse not found or access denied")));
    }

    public Mono<Void> deleteDevice(String id, String ownerId) {
        return deviceRepository.findById(id)
                .flatMap(device -> greenhouseRepository.findById(device.greenhouseId())
                        .filter(gh -> gh.ownerId().equals(ownerId))
                        .flatMap(gh -> deviceRepository.delete(device)))
                .then();
    }
}
