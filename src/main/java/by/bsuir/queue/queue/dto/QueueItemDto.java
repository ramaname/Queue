package by.bsuir.queue.queue.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class QueueItemDto {
    private int position;
    private String userFullName;
    private String category; 
    private int wantedCount;
    private int passesDone;
    private boolean isCurrent;
}