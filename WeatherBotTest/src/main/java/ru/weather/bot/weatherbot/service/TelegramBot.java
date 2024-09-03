package ru.weather.bot.weatherbot.service;

import jakarta.validation.constraints.NotNull;
import lombok.SneakyThrows;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Service;
import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.api.methods.BotApiMethod;
import org.telegram.telegrambots.meta.api.methods.ParseMode;
import org.telegram.telegrambots.meta.api.methods.commands.SetMyCommands;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.methods.send.SendPhoto;
import org.telegram.telegrambots.meta.api.methods.updatingmessages.EditMessageText;
import org.telegram.telegrambots.meta.api.objects.InputFile;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.commands.scope.BotCommandScopeDefault;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
import ru.weather.bot.weatherbot.config.BotConfig;
import ru.weather.bot.weatherbot.enums.BotCommand;
import ru.weather.bot.weatherbot.enums.BotLanguage;
import ru.weather.bot.weatherbot.json.ProcessingData;
import ru.weather.bot.weatherbot.json.ReceiveData;
import ru.weather.bot.weatherbot.json.WeatherMapper;
import ru.weather.bot.weatherbot.models.BotModel;
import ru.weather.bot.weatherbot.models.Messages;
import ru.weather.bot.weatherbot.models.database.Client;
import ru.weather.bot.weatherbot.models.database.ClientService;

import java.io.File;
import java.util.Deque;
import java.util.List;
import java.util.function.Supplier;

import static ru.weather.bot.weatherbot.service.MessageSend.createMessage;

@Service
@Scope(scopeName = "prototype")
public class TelegramBot extends TelegramLongPollingBot
{
    private final BotConfig botConfig;
    private final WeatherMapper weatherMapper;
    private final Deque<BotApiMethod<?>> stackMessages;
    private final ReceiveData receiveData;
    private final ProcessingData processingData;
    private final ClientService service;
    private BotLanguage botLanguage;
    private BotCommand botCommand;

    @Autowired
    public TelegramBot(BotConfig botConfig, WeatherMapper weatherMapper, Deque<BotApiMethod<?>> stackMessages,
                       ReceiveData receiveData, ProcessingData processingData, ClientService service) throws TelegramApiException
    {
        super(botConfig.getBotToken());
        this.botConfig = botConfig;
        this.weatherMapper = weatherMapper;
        this.stackMessages = stackMessages;
        this.receiveData = receiveData;
        this.processingData = processingData;
        this.service = service;
        botLanguage = BotLanguage.GERMAN;
        sendApiMethod(new SetMyCommands(BotModel.commandListForBotMenu(), new BotCommandScopeDefault(), null));
    }

    @Override
    public String getBotUsername()
    {
        return botConfig.getBotName();
    }

    @Override
    public void onUpdateReceived(Update update)
    {
        if (update.hasMessage() && update.getMessage().hasText())
        {
            stackMessages.clear();
            String message = update.getMessage().getText();
            Long chatId = update.getMessage().getChatId();
            SendMessage sendMessage = createMessage(chatId, "");
            botConfig.setMessageId(update.getMessage().getMessageId() + 1);
            if (botCommand == null && !message.equals(BotCommand.START.getCommand()))
            {
                executeMessage(createMessage(chatId, Messages.EN_UNSUCCESSFUL_RELOAD_EVENT_HANDLING), null);
                return;
            }
            if (!existCommand(update))
                weatherRequest(sendMessage, message, chatId);
        } else if (update.hasCallbackQuery())
        {
            String callbackData = update.getCallbackQuery().getData();
            Integer messageId = update.getCallbackQuery().getMessage().getMessageId();
            Long chatId = update.getCallbackQuery().getMessage().getChatId();

            EditMessageText text = new EditMessageText();
            text.setChatId(chatId);
            text.setMessageId(messageId);

            String unsuccessfulEvent = switch (botLanguage)
            {
                case RUSSIAN -> Messages.EN_UNSUCCESSFUL_EVENT_HANDLING;
                case ENGLISH -> Messages.EN_UNSUCCESSFUL_EVENT_HANDLING;
                case CHINESE -> Messages.CN_UNSUCCESSFUL_EVENT_HANDLING;
                case GERMAN -> Messages.DE_UNSUCCESSFUL_EVENT_HANDLING;
            };

            if (botConfig.getMessageId() == null || (messageId.intValue() != botConfig.getMessageId().intValue()))
            {
                executeMessage(createMessage(chatId, unsuccessfulEvent), null);
                return;
            }
            callBackEvent(text, callbackData, unsuccessfulEvent, chatId);
        }
    }

    public void callBackEvent(EditMessageText editText, String callBack, String unsuccessfulEvent, long chatId)
    {
        String cityName = receiveData.getWeatherConfig().getCityName();
        switch (callBack)
        {
            case "RuLang":
                languageSwitching(BotLanguage.RUSSIAN, editText);
                break;
            case "EnLang":
                languageSwitching(BotLanguage.ENGLISH, editText);
                break;
            case "CnLang":
                languageSwitching(BotLanguage.CHINESE, editText);
                break;
            case "DeLang":
                languageSwitching(BotLanguage.GERMAN, editText);
                break;
            case "DetailedWeather":
                stackMessages.push(stackMessages.peek());
                editMessage(weatherMapper.detailedWeather(cityName, botLanguage), editText, BotModel::getBackButton);
                return;
            case "DetailedWeatherForecast":
                stackMessages.push(stackMessages.peek());
                editMessage(weatherMapper.detailedWeatherForecast(cityName, receiveData.getWeatherConfig().getCurrentDay(), botLanguage),
                        editText, BotModel::getBackButton);
                return;
            case "WF_1":
                stackMessages.push(stackMessages.peek());
                receiveData.getWeatherConfig().setCurrentDay(6);
                editMessage(weatherMapper.weatherForecastDay(cityName, 8 - 2, botLanguage), editText, BotModel::buttonWF);
                return;
            case "WF_2":
                stackMessages.push(stackMessages.peek());
                receiveData.getWeatherConfig().setCurrentDay(14);
                editMessage(weatherMapper.weatherForecastDay(cityName, 2 * 8 - 2, botLanguage), editText, BotModel::buttonWF);
                return;
            case "WF_3":
                stackMessages.push(stackMessages.peek());
                receiveData.getWeatherConfig().setCurrentDay(22);
                editMessage(weatherMapper.weatherForecastDay(cityName, 3 * 8 - 2, botLanguage), editText, BotModel::buttonWF);
                return;
            case "WF_4":
                stackMessages.push(stackMessages.peek());
                receiveData.getWeatherConfig().setCurrentDay(30);
                editMessage(weatherMapper.weatherForecastDay(cityName, 4 * 8 - 2, botLanguage), editText, BotModel::buttonWF);
                return;
            case "WF_5":
                stackMessages.push(stackMessages.peek());
                receiveData.getWeatherConfig().setCurrentDay(38);
                editMessage(weatherMapper.weatherForecastDay(cityName, 5 * 8 - 2, botLanguage), editText, BotModel::buttonWF);
                return;
            case "Back":
                if (stackMessages.isEmpty())
                {
                    executeMessage(createMessage(chatId, unsuccessfulEvent), null);
                    return;
                }
                if (stackMessages.peek() instanceof SendMessage)
                {
                    SendMessage send = (SendMessage) stackMessages.pop();
                    editText.setReplyMarkup((InlineKeyboardMarkup) send.getReplyMarkup());
                    editMessage(send.getText(), editText, null);
                    return;
                } else if (stackMessages.peek() instanceof EditMessageText)
                {
                    EditMessageText send = (EditMessageText) stackMessages.pop();
                    editText.setReplyMarkup(send.getReplyMarkup());
                    editMessage(send.getText(), editText, null);
                    return;
                }
            default:
                botCommand.defaultCommand(this, chatId, botLanguage);
        }
        if (botCommand != null && botCommand.getCommand().equals(BotCommand.START.getCommand()))
            botCommand.helpCommand(this, chatId, botLanguage);
    }

    public void weatherRequest(SendMessage sendMessage, String message, long chatId)
    {
        String input_error = switch (botLanguage)
        {
            case RUSSIAN -> Messages.RU_CITY_INPUT_ERROR;
            case ENGLISH -> Messages.EN_CITY_INPUT_ERROR;
            case CHINESE -> Messages.CN_CITY_INPUT_ERROR;
            case GERMAN -> Messages.DE_CITY_INPUT_ERROR;
        };
        if (botCommand.getCommand().equals(BotCommand.MAP.getCommand()))
            executePhoto(message, chatId, input_error);
        else
        {
            if (message.startsWith("/"))
                botCommand.defaultCommand(this, chatId, botLanguage);
            else
            {
                if (message.matches("^.*\\s\\d{1,2}$"))
                    weatherForecast(sendMessage, message, chatId, input_error);
                else currentWeather(sendMessage, message, chatId, input_error);
            }
        }
    }

    public boolean existCommand(Update update)
    {
        String text = update.getMessage().getText();
        long chatId = update.getMessage().getChatId();
        return switch (text)
        {
            case "/start":
                botCommand = BotCommand.START;
                service.registeredClient(update.getMessage());
                botCommand.startCommand(this, chatId, update.getMessage().getChat().getFirstName());
                yield true;
            case "/help":
                botCommand = BotCommand.HELP;
                botCommand.helpCommand(this, chatId, botLanguage);
                yield true;
            case "/lang":
                botCommand = BotCommand.LANG;
                botCommand.langCommand(this, chatId, botLanguage);
                yield true;
            case "/map":
                botCommand = BotCommand.MAP;
                botCommand.mapCommand(this, chatId, botLanguage);
                yield true;
            default:
                if (text.startsWith("/send ") && service.isAdmin(chatId))
                {
                    String message = text.substring(text.indexOf(" ") + 1);
                    service.findAll().forEach(client -> sendEveryone(client, message));
                    yield true;
                }
                else yield false;
        };
    }
    @SneakyThrows
    private void sendEveryone(Client client, String message)
    {
        sendApiMethod(SendMessage.builder()
                .chatId(client.getChatId())
                .text(message)
                .parseMode(ParseMode.HTML)
                .build());
    }

    public void weatherForecast(SendMessage sendMessage, String message, long chatId, String error)
    {
        String weather = weatherMapper.weatherForecastDispatch(message.toLowerCase(), botLanguage);
        if (weather == null || weather.isEmpty())
            executeMessage(createMessage(chatId, error), null);
        else
        {
            String[] city_days = processingData.splitSpace(message.toLowerCase());
            receiveData.getWeatherConfig().setQuantityDays(Integer.parseInt(city_days[1]));
            receiveData.getWeatherConfig().setCityName(city_days[0]);
            sendMessage.setText(weather);
            stackMessages.push(sendMessage);
            executeMessage(sendMessage, () -> BotModel.getButtonsForecastWeather(receiveData.getWeatherConfig().getQuantityDays()));
        }
    }

    public void currentWeather(SendMessage sendMessage, String message, long chatId, String error)
    {
        String weather = weatherMapper.weatherDispatch(message.toLowerCase(), botLanguage);
        if (weather == null || weather.isEmpty())
            executeMessage(createMessage(chatId, error), null);
        else
        {
            sendMessage.setText(weather);
            stackMessages.push(sendMessage);
            receiveData.getWeatherConfig().setCityName(message.toLowerCase());
            executeMessage(sendMessage, () -> BotModel.getButtonForDetailedWeather(botLanguage, "DetailedWeather"));
        }
    }

    @SneakyThrows
    public void executePhoto(String message, long chatId, String error)
    {
        File photoMap = receiveData.fetchWeatherMap(message, botLanguage);
        botCommand = BotCommand.DEFAULT;
        if (photoMap == null)
            executeMessage(createMessage(chatId, error), null);
        else
        {
            SendPhoto sendPhoto = new SendPhoto();
            sendPhoto.setChatId(chatId);
            sendPhoto.setPhoto(new InputFile(photoMap));
            execute(sendPhoto);
            photoMap.delete();
            executeMessage(createMessage(chatId, Messages.infoWeatherWithMap(botLanguage, processingData.convertCityNameCorrectly(message))), null);
        }
    }

    @SneakyThrows
    public void editMessage(String text, @NotNull EditMessageText messageText, Supplier<List<List<InlineKeyboardButton>>> message)
    {
        messageText.setText(text);
        messageText.setParseMode(ParseMode.HTML);
        if (message != null)
        {
            var markup = new InlineKeyboardMarkup();
            List<List<InlineKeyboardButton>> rows = message.get();
            markup.setKeyboard(rows);
            messageText.setReplyMarkup(markup);
        }
        sendApiMethod(messageText);
    }

    @SneakyThrows
    public void executeMessage(SendMessage sendMessage, Supplier<List<List<InlineKeyboardButton>>> message)
    {
        if (message != null)
        {
            var markup = new InlineKeyboardMarkup();
            List<List<InlineKeyboardButton>> rows = message.get();
            markup.setKeyboard(rows);
            sendMessage.setReplyMarkup(markup);
        }
        sendApiMethod(sendMessage);
    }

    private void languageSwitching(BotLanguage language, EditMessageText messageText)
    {
        if (botLanguage == language && !botCommand.getCommand().equals(BotCommand.START.getCommand()))
            editMessage(switch (language)
            {
                case RUSSIAN -> Messages.RU_THE_LANGUAGE_IS_ALREADY_THERE;
                case ENGLISH -> Messages.EN_THE_LANGUAGE_IS_ALREADY_THERE;
                case CHINESE -> Messages.CN_THE_LANGUAGE_IS_ALREADY_THERE;
                case GERMAN -> Messages.DE_THE_LANGUAGE_IS_ALREADY_THERE;
            }, messageText, null);
        else
        {
            editMessage(switch (language)
            {
                case RUSSIAN -> Messages.RU_FURTHER_COMMUNICATION;
                case ENGLISH -> Messages.EN_FURTHER_COMMUNICATION;
                case CHINESE -> Messages.CN_FURTHER_COMMUNICATION;
                case GERMAN -> Messages.DE_FURTHER_COMMUNICATION;
            }, messageText, null);
            botLanguage = language;
            BotModel.language = botLanguage;
        }
    }
}