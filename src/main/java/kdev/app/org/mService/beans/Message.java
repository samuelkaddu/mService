package kdev.app.org.mService.beans;

import lombok.*;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
@ToString
public class Message {
    private String message;
    private String address;
    private String t_name;
    private Long id;
    private boolean isRetry;
}
