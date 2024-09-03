package ru.weather.bot.weatherbot.models;

import org.telegram.telegrambots.meta.api.objects.commands.BotCommand;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton;
import ru.weather.bot.weatherbot.enums.BotLanguage;

import java.util.ArrayList;
import java.util.List;

public class BotModel
{
    public static BotLanguage language;
    public static List<List<InlineKeyboardButton>> getRowsForScreenButton()
    {
        List<List<InlineKeyboardButton>> rows = new ArrayList<>();
        List<InlineKeyboardButton> row1 = new ArrayList<>();
        List<InlineKeyboardButton> row2 = new ArrayList<>();
        var ruLang = new InlineKeyboardButton();
        ruLang.setText("Russian");
        ruLang.setCallbackData("RuLang");

        var enLang = new InlineKeyboardButton();
        enLang.setText("English");
        enLang.setCallbackData("EnLang");

        var cnLang = new InlineKeyboardButton();
        cnLang.setText("Chinese");
        cnLang.setCallbackData("CnLang");

        var deLang = new InlineKeyboardButton();
        deLang.setText("German");
        deLang.setCallbackData("DeLang");

        row1.add(ruLang);
        row1.add(enLang);
        row2.add(cnLang);
        row2.add(deLang);

        rows.add(row1);
        rows.add(row2);
        return rows;
    }

    public static List<List<InlineKeyboardButton>> getButtonForDetailedWeather(BotLanguage language, String callBackData)
    {
        List<List<InlineKeyboardButton>> rows = new ArrayList<>();
        List<InlineKeyboardButton> row = new ArrayList<>();
        var detailedForecast = new InlineKeyboardButton();
        detailedForecast.setText(switch (language)
        {
            case RUSSIAN -> Messages.RU_DETAILED_FORECAST;
            case ENGLISH -> Messages.EN_DETAILED_FORECAST;
            case CHINESE -> Messages.CN_DETAILED_FORECAST;
            case GERMAN -> Messages.DE_DETAILED_FORECAST;
        });
        detailedForecast.setCallbackData(callBackData);
        row.add(detailedForecast);
        rows.add(row);
        return rows;
    }

    public static List<BotCommand> commandListForBotMenu()
    {
        List<BotCommand> commandList = new ArrayList<>();
        commandList.add(new BotCommand(ru.weather.bot.weatherbot.enums.BotCommand.START.getCommand(), Messages.START_COMMAND_DESCRIPTION));
        commandList.add(new BotCommand(ru.weather.bot.weatherbot.enums.BotCommand.HELP.getCommand(), Messages.HELP_COMMAND_DESCRIPTION));
        commandList.add(new BotCommand(ru.weather.bot.weatherbot.enums.BotCommand.LANG.getCommand(), Messages.LANG_COMMAND_DESCRIPTION));
        commandList.add(new BotCommand(ru.weather.bot.weatherbot.enums.BotCommand.MAP.getCommand(), Messages.MAP_COMMAND_DESCRIPTION));
        return commandList;
    }

    public static List<List<InlineKeyboardButton>> getButtonsForecastWeather(int countDays)
    {
        List<List<InlineKeyboardButton>> rows = new ArrayList<>();
        List<InlineKeyboardButton> row = new ArrayList<>();
        for (int i = 0; i < countDays; i++)
        {
            InlineKeyboardButton button = new InlineKeyboardButton();
            button.setText(String.valueOf(i + 1));
            button.setCallbackData("WF_" + (i + 1));
            row.add(button);
            if ((i + 1) % 4 == 0)
            {
                rows.add(row);
                row = new ArrayList<>();
            }
        }
        rows.add(row);
        return rows;
    }

    public static List<List<InlineKeyboardButton>> buttonWF()
    {
        List<List<InlineKeyboardButton>> rows = getButtonForDetailedWeather(language, "DetailedWeatherForecast");
        rows.addAll(getBackButton());
        return rows;
    }

    public static List<List<InlineKeyboardButton>> getBackButton()
    {
        List<List<InlineKeyboardButton>> rows = new ArrayList<>();
        List<InlineKeyboardButton> row = new ArrayList<>();
        var button = new InlineKeyboardButton();
        button.setText(switch (language)
        {
            case RUSSIAN -> "Назад";
            case ENGLISH -> "Back";
            case CHINESE -> "向后";
            case GERMAN -> "Zurückgehen";
        });
        button.setCallbackData("Back");
        row.add(button);
        rows.add(row);
        return rows;
    }
}