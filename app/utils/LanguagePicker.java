package utils;

import play.i18n.Lang;

/**
 *
 * @author Luca Galli <lgalli@elet.polimi.it>
 */
public class LanguagePicker {

    public static String platformLang = "";
    public static Lang playLang;
    static Integer langCode = 0;

    public static void setLanguage(Lang retrieved) {
        switch (AllowedLang.valueOf(retrieved.language())) {
            case it:
                platformLang = "ita";
                langCode = 21;
                break;
            case en:
                platformLang = "eng";
                langCode = 18;
                break;
            default:
                platformLang = "undefined";
                langCode = 0;
        }
        //[TODO] HAS TO BE FIXED, NOW IT'S FIXED TO ITA
        setLanguage("en");
    }

    public static void setLanguage(String lang) {
        switch (AllowedLang.valueOf(lang)) {
            case it:
                platformLang = "ita";
                break;
            case en:
                platformLang = "eng";
                break;
            default:
                platformLang = "undefined";
        }
        playLang = new Lang(play.api.i18n.Lang.apply(lang));
    }

    public static String retrieveIsoCode() {
        return platformLang;
    }

    public static Lang retrieveLocale() {
        return playLang;
    }

    public static Integer getLangCode() {
        return langCode;
    }
}

enum AllowedLang {

    it, en
}
