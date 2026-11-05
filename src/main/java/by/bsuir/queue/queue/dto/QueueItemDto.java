package by.bsuir.queue.queue.dto;

public record QueueItemDto(
        int position,
        String userFullName,
        String category,
        int wantedCount,
        int passesDone,
        boolean current,
        boolean mine) { }
