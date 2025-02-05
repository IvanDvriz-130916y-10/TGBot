package io.proj5ct.SpringDemoBot.service;

import io.proj5ct.SpringDemoBot.config.BotConfig;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Component
public class TelegramBot extends TelegramLongPollingBot {

    final BotConfig config;

    // Карта для хранения состояния пользователя
    private final Map<Long, String> userStates = new HashMap<>();
    // Список для хранения оценок
    private final List<Integer> ratings = new ArrayList<>();
    // ID целевого чата
    private final String targetChatId = "-4608379231";

    public TelegramBot(BotConfig config) {
        this.config = config;
        // Пересчитываем оценки при запуске бота
        recalculateAverageRating();
    }

    @Override
    public String getBotUsername() {
        return config.getBotName();
    }

    @Override
    public String getBotToken() {
        return config.getToken();
    }

    @Override
    public void onUpdateReceived(Update update) {
        if (update.hasMessage() && update.getMessage().hasText()) { // Обработка текстового сообщения
            String messageText = update.getMessage().getText();
            long chatId = update.getMessage().getChatId();

            if (String.valueOf(chatId).equals(targetChatId) && "Покажи средний балл".equalsIgnoreCase(messageText)) {
                // Если сообщение "Покажи средний балл" пришло из целевого чата
                sendAverageRating(chatId);
            } else if (userStates.containsKey(chatId) && "WAITING_FOR_FEEDBACK".equals(userStates.get(chatId))) {
                String senderName = update.getMessage().getChat().getFirstName(); // Получаем имя пользователя
                String senderLink = "[" + senderName + "](tg://user?id=" + update.getMessage().getChat().getId() + ")"; // Создаём ссылку на пользователя

                // Отправляем отзыв в целевой чат
                forwardMessage(targetChatId, senderLink + ": " + messageText);

                // Благодарим пользователя
                sendMessage(chatId, "Спасибо за ваш отзыв! Вы помогаете нам стать лучше!");
                userStates.remove(chatId); // Сбрасываем состояние
                showMainMenu(chatId); // Возвращаем пользователя в главное меню
            } else if ("/start".equals(messageText)) {
                showMainMenu(chatId); // Показываем главное меню
            }
        } else if (update.hasCallbackQuery()) { // Обработка нажатия кнопки
            String callbackData = update.getCallbackQuery().getData();
            long chatId = update.getCallbackQuery().getMessage().getChatId();

            switch (callbackData) {
                case "new_feedback": // Кнопка "Оставить новый отзыв"
                    userStates.put(chatId, "WAITING_FOR_FEEDBACK");
                    sendMessage(chatId, "Пожалуйста, напишите ваш отзыв.");
                    break;
                case "rate_food": // Кнопка "Оценка по питанию"
                    userStates.put(chatId, "WAITING_FOR_RATING");
                    sendRatingButtons(chatId, "Поставьте оценку от 1 до 5:");
                    break;
                case "rating_1":
                case "rating_2":
                case "rating_3":
                case "rating_4":
                case "rating_5":
                    int rating = Integer.parseInt(callbackData.split("_")[1]); // Извлекаем выбранную оценку
                    ratings.add(rating); // Сохраняем оценку
                    recalculateAverageRating(); // Пересчитываем средний балл
                    sendMessage(chatId, "Спасибо за вашу оценку. Вы помогаете нам стать лучше!");
                    userStates.remove(chatId); // Сбрасываем состояние
                    showMainMenu(chatId); // Возвращаем пользователя в главное меню
                    break;
                default:
                    break;
            }
        }
    }

    private void showMainMenu(long chatId) {
        SendMessage message = new SendMessage();
        message.setChatId(String.valueOf(chatId));
        message.setText("Добро пожаловать! Выберите действие:");

        // Создаем кнопки главного меню
        InlineKeyboardMarkup keyboardMarkup = new InlineKeyboardMarkup();
        List<List<InlineKeyboardButton>> keyboard = new ArrayList<>();

        // Кнопка "Оставить новый отзыв"
        List<InlineKeyboardButton> row1 = new ArrayList<>();
        InlineKeyboardButton feedbackButton = new InlineKeyboardButton();
        feedbackButton.setText("Оставить новый отзыв");
        feedbackButton.setCallbackData("new_feedback");
        row1.add(feedbackButton);

        // Кнопка "Оценка по питанию"
        List<InlineKeyboardButton> row2 = new ArrayList<>();
        InlineKeyboardButton rateButton = new InlineKeyboardButton();
        rateButton.setText("Оценка по питанию");
        rateButton.setCallbackData("rate_food");
        row2.add(rateButton);

        keyboard.add(row1);
        keyboard.add(row2);
        keyboardMarkup.setKeyboard(keyboard);
        message.setReplyMarkup(keyboardMarkup);

        try {
            execute(message);
        } catch (TelegramApiException e) {
            e.printStackTrace();
        }
    }

    private void sendRatingButtons(long chatId, String text) {
        SendMessage message = new SendMessage();
        message.setChatId(String.valueOf(chatId));
        message.setText(text);

        // Создаем кнопки для выбора оценки
        InlineKeyboardMarkup keyboardMarkup = new InlineKeyboardMarkup();
        List<List<InlineKeyboardButton>> keyboard = new ArrayList<>();

        List<InlineKeyboardButton> row = new ArrayList<>();
        for (int i = 1; i <= 5; i++) {
            InlineKeyboardButton button = new InlineKeyboardButton();
            button.setText(i + "⭐");
            button.setCallbackData("rating_" + i); // Callback для оценки
            row.add(button);
        }

        keyboard.add(row);
        keyboardMarkup.setKeyboard(keyboard);
        message.setReplyMarkup(keyboardMarkup);

        try {
            execute(message);
        } catch (TelegramApiException e) {
            e.printStackTrace();
        }
    }

    private void sendMessage(long chatId, String textToSend) {
        SendMessage message = new SendMessage();
        message.setChatId(String.valueOf(chatId));
        message.setText(textToSend);

        try {
            execute(message);
        } catch (TelegramApiException e) {
            e.printStackTrace();
        }
    }

    private void sendAverageRating(long chatId) {
        if (ratings.isEmpty()) {
            sendMessage(chatId, "На данный момент нет оценок.");
            return;
        }

        int count = ratings.size();
        double average = ratings.stream().mapToInt(Integer::intValue).average().orElse(0.0);

        String response = String.format("Количество отзывов: %d\nСредний балл: %.2f", count, average);
        sendMessage(chatId, response);
    }

    private void recalculateAverageRating() {
        // Логика для пересчета среднего балла (запускается при изменении оценок)
        if (ratings.isEmpty()) {
            return; // Нет оценок для пересчета
        }

        double average = ratings.stream().mapToInt(Integer::intValue).average().orElse(0.0);
        System.out.println(String.format("Пересчитан средний балл: %.2f (Количество оценок: %d)", average, ratings.size()));
    }

    private void forwardMessage(String targetChatId, String message) {
        SendMessage sendMessage = new SendMessage();
        sendMessage.setChatId(targetChatId);
        sendMessage.setText(message);
        sendMessage.setParseMode("Markdown"); // Включаем Markdown для ссылки

        try {
            execute(sendMessage);
        } catch (TelegramApiException e) {
            e.printStackTrace();
        }
    }
}