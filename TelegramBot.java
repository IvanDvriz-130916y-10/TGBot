package io.proj5ct.SpringDemoBot.service;

import io.proj5ct.SpringDemoBot.config.BotConfig;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
@Component
public class TelegramBot extends TelegramLongPollingBot {

    final BotConfig config;
    public TelegramBot(BotConfig config) {
        this.config = config;
    }
    @Override
    public String getBotUsername () {
        return config.getBotName ();
    }
    @Override
    public String getBotToken () {
        return config.getToken ();
    }
    @Override
    public void onUpdateReceived(Update update) {
        if (update.hasMessage() && update.getMessage().hasText()) {
            String messageText = update.getMessage().getText();
            long chatId = update.getMessage().getChatId();
            String targetChatId = "-4563264990"; // Замените на ID целевой беседы в виде строки
            String senderName = update.getMessage().getChat().getFirstName(); // Получаем имя пользователя, который отправил сообщение
            String senderLink = "[" + senderName + "](tg://user?id=" + update.getMessage().getChat().getId() + ")"; // Создаём ссылку на пользователя

            switch (messageText) {
                case "/start":
                    startCommandReceived(chatId, senderName);
                    break;
                default:
                    sendMessage(chatId, "Ваш отзыв помогает нам стать лучше. Спасибо! ");
                    forwardMessage(String.valueOf(chatId), targetChatId, senderLink + ": " + messageText); // Пересылаем сообщение в целевую беседу с именем пользователя в виде ссылки
                    break;
            }
        }
    }

    private void startCommandReceived(long chatId, String name) {
        String answer = "Здравствуйте, " + name + ", просим Вас написать отзыв связанный с питанием в столовой";
        sendMessage(chatId, answer);
    }

    private void sendMessage(long chatId, String textToSend) {
        SendMessage message = new SendMessage();
        message.setChatId(String.valueOf(chatId));
        message.setText(textToSend);
        message.setParseMode("Markdown"); // Включаем режим Markdown для отображения ссылки

        try {
            execute(message);
        }
        catch (TelegramApiException e) {
            e.printStackTrace();
        }
    }

    private void forwardMessage(String chatId, String targetChatId, String message) {
        SendMessage sendMessage = new SendMessage();
        sendMessage.setChatId(targetChatId);
        sendMessage.setText(message);
        sendMessage.setParseMode("Markdown"); // Включаем режим Markdown для отображения ссылки

        try {
            execute(sendMessage);
        }
        catch (TelegramApiException e) {
            e.printStackTrace();
        }
    }
}
