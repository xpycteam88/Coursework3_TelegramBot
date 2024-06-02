package pro.sky.telegrambot.listener;

import com.pengrad.telegrambot.TelegramBot;
import com.pengrad.telegrambot.UpdatesListener;
import com.pengrad.telegrambot.model.Update;
import com.pengrad.telegrambot.request.SendMessage;
import com.pengrad.telegrambot.response.SendResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import pro.sky.telegrambot.model.NotificationTask;
import pro.sky.telegrambot.repositories.NotificationTasRepository;

import javax.annotation.PostConstruct;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
public class TelegramBotUpdatesListener implements UpdatesListener {

    private static final Logger logger = LoggerFactory.getLogger(TelegramBotUpdatesListener.class);

    private static final String WELCOME_MESSAGE = "Привет!" +
            " Введи по порядку время и название задачи в формате: 01.01.2022 20:00 Сделать домашнюю работу";
    private static final Pattern PATTERN = Pattern.compile("([0-9\\.\\:\\s]{16})(\\s)([\\W+]+)");
    private static final DateTimeFormatter DATE_TIME_FORMATTER= DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm");
    private final TelegramBot telegramBot;
    private final NotificationTasRepository notificationTasRepository;


    public TelegramBotUpdatesListener(TelegramBot telegramBot, NotificationTasRepository notificationTasRepository) {
        this.telegramBot = telegramBot;
        this.notificationTasRepository = notificationTasRepository;
    }

    @PostConstruct
    public void init() {
        telegramBot.setUpdatesListener(this);
    }

    @Override
    public int process(List<Update> updates) {
        updates.forEach(update -> {
            logger.info("Processing update: {}", update);
            var message = update.message();
            if (message != null) {
                var messageText = update.message().text();
                var chatId = update.message().chat().id();

                if (messageText != null && messageText.equals("/start")) {
                    sendMessage(chatId, WELCOME_MESSAGE);
                } else {
                    splitMessage(chatId, messageText);
                }
            }
        });
        return UpdatesListener.CONFIRMED_UPDATES_ALL;
    }

    public void splitMessage(Long chat, String message) {
        Matcher matcher = PATTERN.matcher(message);

        try {
            if (!matcher.matches()) {
                throw new IllegalStateException("Not a perfect String");
            }
        } catch (IllegalStateException e) {
            logger.error("Could not parse MAT date {}, expected format [{}].", matcher, message);
            sendMessage(chat, "Недопустимые символы");
            return;
        }

        var dateTime = matcher.group(1);
        var reminderText = matcher.group(3);
        LocalDateTime reminderClock;
        try {
            reminderClock = LocalDateTime.parse(dateTime, DATE_TIME_FORMATTER);
        } catch (DateTimeParseException e) {
            logger.error("Could not parse MAT date {}, expected format [{}].", dateTime, DATE_TIME_FORMATTER);
            sendMessage(chat, "Неправильный формат даты");
            return;
        }

        NotificationTask task = new NotificationTask();
        task.setChatId(chat);
        task.setTaskText(reminderText);
        task.setTaskClock(reminderClock);
        notificationTasRepository.save(task);
        logger.info("Task has been saved {}", task);
        sendMessage(chat, "Задача добавлена");
    }

    private void sendMessage(Long chat, String text) {
        SendMessage message = new SendMessage(chat, text);

        SendResponse response = telegramBot.execute(message);
        logger.info("Response: {}", response.isOk());
        logger.info("Error code: {}", response.errorCode());
    }

}
