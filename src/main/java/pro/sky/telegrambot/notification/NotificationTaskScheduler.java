package pro.sky.telegrambot.notification;

import com.pengrad.telegrambot.TelegramBot;
import com.pengrad.telegrambot.request.SendMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import pro.sky.telegrambot.listener.TelegramBotUpdatesListener;
import pro.sky.telegrambot.repositories.NotificationTasRepository;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;

@Service
public class NotificationTaskScheduler {

    private final static Logger logger = LoggerFactory.getLogger(NotificationTaskScheduler.class);

    private final TelegramBot telegramBot;
    private final NotificationTasRepository repository;

    public NotificationTaskScheduler(TelegramBot telegramBot, NotificationTasRepository repository) {
        this.telegramBot = telegramBot;
        this.repository = repository;
    }

    @Scheduled(cron = "0 0/1 * * * *")
    public void sendAndDeleteNotification(){
        repository.findNotificationTaskByTaskClock(LocalDateTime.now().truncatedTo(ChronoUnit.MINUTES))
                .forEach(task -> {
                    telegramBot.execute(new SendMessage(task.getChatId(), task.getTaskText()));
                    logger.info("Message has been sent");
                    repository.deleteById(task.getId());
                });



    }
}
