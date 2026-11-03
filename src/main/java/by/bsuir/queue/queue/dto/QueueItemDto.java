package by.bsuir.queue.queue.dto;

/**
 * Строка очереди для фронтенда.
 * current — сейчас сдаёт этот студент; mine — это запись текущего пользователя
 * (фронтенду больше не нужно угадывать себя по ФИО).
 */
public record QueueItemDto(
        int position,
        String userFullName,
        String category,
        int wantedCount,
        int passesDone,
        boolean current,
        boolean mine) { }
