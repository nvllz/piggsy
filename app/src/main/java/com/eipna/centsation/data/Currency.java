package com.eipna.centsation.data;

import java.text.NumberFormat;

public enum Currency {
    AFGHANI("Afghan Afghani", "AFN", "؋"),
    LEK("Albanian Lek", "ALL", "L"),
    ALGERIAN_DINAR("Algerian Dinar", "DZD", "د.ج"),
    ANGUILLA_DOLLAR("Anguillian Dollar", "XCD", "$"),
    ARGENTINE_PESO("Argentine Peso", "ARS", "$"),
    ARMENIAN_DRAM("Armenian Dram", "AMD", "֏"),
    AUSTRALIAN_DOLLAR("Australian Dollar", "AUD", "$"),
    AZERBAIJANI_MANAT("Azerbaijani Manat", "AZN", "₼"),
    BAHAMIAN_DOLLAR("Bahamian Dollar", "BSD", "$"),
    BAHRAINI_DINAR("Bahraini Dinar", "BHD", ".د.ب"),
    BANGLADESHI_TAKA("Bangladeshi Taka", "BDT", "৳"),
    BARBADOS_DOLLAR("Barbadian Dollar", "BBD", "$"),
    BELARUSIAN_RUBLE("Belarusian Ruble", "BYN", "Br"),
    BELIZE_DOLLAR("Belize Dollar", "BZD", "$"),
    BERMUDIAN_DOLLAR("Bermudian Dollar", "BMD", "$"),
    BHUTANESE_NGULTRUM("Bhutanese Ngultrum", "BTN", "Nu."),
    BOLIVIAN_BOLIVIANO("Bolivian Boliviano", "BOB", "Bs."),
    BOSNIAN_CONVERTIBLE_MARK("Bosnian Convertible Mark", "BAM", "KM"),
    BOTSWANA_PULA("Botswana Pula", "BWP", "P"),
    BRAZILIAN_REAL("Brazilian Real", "BRL", "R$"),
    BRITISH_POUND("British Pound Sterling", "GBP", "£"),
    BRUNEI_DOLLAR("Brunei Dollar", "BND", "$"),
    BULGARIAN_LEV("Bulgarian Lev", "BGN", "лв."),
    BURUNDIAN_FRANC("Burundian Franc", "BIF", "Fr"),
    CAMBODIAN_RIEL("Cambodian Riel", "KHR", "៛"),
    CANADIAN_DOLLAR("Canadian Dollar", "CAD", "$"),
    CAPE_VERDEAN_ESCUDO("Cape Verdean Escudo", "CVE", "$"),
    CENTRAL_AFRICAN_CFA_FRANC("Central African CFA Franc", "XAF", "FCFA"),
    CHADIAN_CFA_FRANC("Chadian CFA Franc", "XAF", "FCFA"),
    CHILEAN_PESO("Chilean Peso", "CLP", "$"),
    CHINESE_YUAN("Chinese Yuan", "CNY", "¥"),
    COLOMBIAN_PESO("Colombian Peso", "COP", "$"),
    COMORO_FRANC("Comorian Franc", "KMF", "CF"),
    CONGOLESE_FRANC("Congolese Franc", "CDF", "FC"),
    COSTA_RICAN_COLON("Costa Rican Colón", "CRC", "₡"),
    CROATIAN_KUNA("Croatian Kuna", "HRK", "kn"),
    CUBAN_PESO("Cuban Peso", "CUP", "$"),
    CZECH_KORUNA("Czech Koruna", "CZK", "Kč"),
    DANISH_KRONE("Danish Krone", "DKK", "kr"),
    DJIBOUTI_FRANC("Djiboutian Franc", "DJF", "Fdj"),
    DOMINICAN_PESO("Dominican Peso", "DOP", "$"),
    EAST_CARIBBEAN_DOLLAR("East Caribbean Dollar", "XCD", "$"),
    EGYPTIAN_POUND("Egyptian Pound", "EGP", "ج.م"),
    EL_SALVADOR_COLON("El Salvador Colón", "SVC", "$"),
    EQUATORIAL_GUINEAN_FRANC("Equatorial Guinean Franc", "XAF", "FCFA"),
    ERITREAN_NAKFA("Eritrean Nakfa", "ERN", "Nfk"),
    ESTONIAN_KROON("Estonian Kroon", "EEK", "kr"),
    ETHIOPIAN_BIRR("Ethiopian Birr", "ETB", "Br"),
    EURO("Euro", "EUR", "€"),
    FRANC("Fijian Dollar", "FJD", "$"),
    FORINT("Hungarian Forint", "HUF", "Ft"),
    GAMBIA_DALASI("Gambian Dalasi", "GMD", "D"),
    GEORGIAN_LARI("Georgian Lari", "GEL", "₾"),
    GERMAN_EURO("German Euro", "EUR", "€"),
    GHANAIAN_CEDI("Ghanaian Cedi", "GHS", "₵"),
    GIBRALTAR_POUND("Gibraltar Pound", "GIP", "£"),
    GREEK_EURO("Greek Euro", "EUR", "€"),
    GUATEMALAN_QUETZAL("Guatemalan Quetzal", "GTQ", "Q"),
    GUINEAN_FRANC("Guinean Franc", "GNF", "FG"),
    GUYANESE_DOLLAR("Guyanese Dollar", "GYD", "$"),
    HAITIAN_GOURDE("Haitian Gourde", "HTG", "G"),
    HONDURAN_LEMPIRA("Honduran Lempira", "HNL", "L"),
    HUNGARIAN_FORINT("Hungarian Forint", "HUF", "Ft"),
    ICELANDIC_KRONA("Icelandic Króna", "ISK", "kr"),
    INDIAN_RUPEE("Indian Rupee", "INR", "₹"),
    INDONESIAN_RUPIAH("Indonesian Rupiah", "IDR", "Rp"),
    IRANIAN_RIAL("Iranian Rial", "IRR", "﷼"),
    IRAQI_DINAR("Iraqi Dinar", "IQD", "ع.د"),
    ISRAELI_NEW_SHEKEL("Israeli New Shekel", "ILS", "₪"),
    ITALIAN_EURO("Italian Euro", "EUR", "€"),
    JAMAICAN_DOLLAR("Jamaican Dollar", "JMD", "$"),
    JAPANESE_YEN("Japanese Yen", "JPY", "¥"),
    JORDANIAN_DINAR("Jordanian Dinar", "JOD", "د.أ"),
    KAZAKHSTANI_TENGE("Kazakhstani Tenge", "KZT", "₸"),
    KENYAN_SHILLING("Kenyan Shilling", "KES", "KSh"),
    KOREAN_WON("South Korean Won", "KRW", "₩"),
    KOSOVO_EURO("Kosovo Euro", "EUR", "€"),
    KUWAITI_DINAR("Kuwaiti Dinar", "KWD", "د.ك"),
    KYRGYZSTANI_SOM("Kyrgyzstani Som", "KGS", "с"),
    LAO_KIP("Lao Kip", "LAK", "₭"),
    LATVIAN_LATS("Latvian Lats", "LVL", "Ls"),
    LEBANESE_POUND("Lebanese Pound", "LBP", "ل.ل"),
    LESOTHO_LOTI("Lesotho Loti", "LSL", "M"),
    LIBERIAN_DOLLAR("Liberian Dollar", "LRD", "$"),
    LIBYAN_DINAR("Libyan Dinar", "LYD", "ل.د"),
    LITHUANIAN_LITAS("Lithuanian Litas", "LTL", "Lt"),
    MACANESE_PATACA("Macanese Pataca", "MOP", "P"),
    MALAGASY_ARIARY("Malagasy Ariary", "MGA", "Ar"),
    MALAWIAN_KWACHA("Malawian Kwacha", "MWK", "K"),
    MALAYSIAN_RINGGIT("Malaysian Ringgit", "MYR", "RM"),
    MALDIVIAN_RUFIYAA("Maldivian Rufiyaa", "MVR", "Rf"),
    MALI_CFA_FRANC("Malian CFA Franc", "XOF", "CFA"),
    MALTESE_EURO("Maltese Euro", "EUR", "€"),
    MAURITANIAN_OUGUIYA("Mauritanian Ouguiya", "MRU", "UM"),
    MAURITIUS_RUPEE("Mauritian Rupee", "MUR", "₨"),
    MEXICAN_PESO("Mexican Peso", "MXN", "$"),
    MOLDOVAN_LEU("Moldovan Leu", "MDL", "L"),
    MONGOLIAN_TUGRIK("Mongolian Tögrög", "MNT", "₮"),
    MOROCCAN_DIRHAM("Moroccan Dirham", " MAD", "د.م."),
    MOZAMBICAN_METICAL("Mozambican Metical", "MZN", "MT"),
    MYANMAR_KYAT("Myanmar Kyat", "MMK", "K"),
    NAMIBIAN_DOLLAR("Namibian Dollar", "NAD", "$"),
    NEPALESE_RUPEE("Nepalese Rupee", "NPR", "Rs"),
    NETHERLANDS_ANTILLEAN_GUILDER("Netherlands Antillean Guilder", "ANG", "ƒ"),
    NEW_ZEALAND_DOLLAR("New Zealand Dollar", "NZD", "$"),
    NICARAGUAN_CORDOBA("Nicaraguan Córdoba", "NIO", "C$"),
    NIGERIAN_NAIRA("Nigerian Naira", "NGN", "₦"),
    NORTH_KOREAN_WON("North Korean Won", "KPW", "₩"),
    NORWEGIAN_KRONE("Norwegian Krone", "NOK", "kr"),
    OMANI_RIAL("Omani Rial", "OMR", "ر.ع."),
    PAKISTANI_RUPEE("Pakistani Rupee", "PKR", "Rs"),
    PANAMANIAN_BALBOA("Panamanian Balboa", "PAB", "B/."),
    PARAGUAYAN_GUARANI("Paraguayan Guarani", "PYG", "₲"),
    PERUVIAN_NUEVO_SOL("Peruvian Nuevo Sol", "PEN", "S/"),
    PHILIPPINE_PESO("Philippine Peso", "PHP", "₱"),
    POLISH_ZLOTY("Polish Zloty", "PLN", "zł"),
    QATARI_RIAL("Qatari Rial", "QAR", "ر.ق"),
    ROMANIAN_LEU("Romanian Leu", "RON", "lei"),
    RUSSIAN_RUBLE("Russian Ruble", "RUB", "₽"),
    RWANDAN_FRANC("Rwandan Franc", "RWF", "FRw"),
    SAINT_HELENA_POUND("Saint Helena Pound", "SHP", "£"),
    SAMOAN_TALA("Samoan Tala", "WST", "T"),
    SAUDI_RIAL("Saudi Riyal", "SAR", "ر.س"),
    SERBIAN_DINAR("Serbian Dinar", "RSD", "дин."),
    SINGAPORE_DOLLAR("Singapore Dollar", "SGD", "$"),
    SLOVAK_KORUNA("Slovak Koruna", "SKK", "Sk"),
    SLOVENIAN_TOLAR("Slovenian Tolar", "SIT", "SIT"),
    SOLOMON_ISLANDS_DOLLAR("Solomon Islands Dollar", "AUD", "$"),
    SOMALI_SHILLING("Somali Shilling", "SOS", "S"),
    SOUTH_AFRICAN_RAND("South African Rand", "ZAR", "R"),
    SOUTH_SUDANESE_POUND("South Sudanese Pound", "SSP", "£"),
    SPANISH_EURO("Spanish Euro", "EUR", "€"),
    SRI_LANKAN_RUPEE("Sri Lankan Rupee", "LKR", "Rs"),
    SUDANESE_POUND("Sudanese Pound", "SDG", "ج.س."),
    SWEDISH_KRONA("Swedish Krona", "SEK", "kr"),
    SWISS_FRANC("Swiss Franc", "CHF", "CHF"),
    SYRIAN_POUND("Syrian Pound", "SYP", "ل.س"),
    TAIWANESE_DOLLAR("New Taiwan Dollar", "TWD", "NT$"),
    TANZANIAN_SHILLING("Tanzanian Shilling", "TZS", "TSh"),
    THAI_BAHT("Thai Baht", "THB", "฿"),
    TOGOLESE_CFA_FRANC("Togolese CFA Franc", "XOF", "CFA"),
    TONGAN_PAANGA("Tongan Paʻanga", "TOP", "T$"),
    TRINIDAD_AND_TOBAGO_DOLLAR("Trinidad and Tobago Dollar", "TTD", "$"),
    TUNISIAN_DINAR("Tunisian Dinar", "TND", "د.ت"),
    TURKISH_LIRA("Turkish Lira", "TRY", "₺"),
    TURKMENISTANI_MANAT("Turkmenistani Manat", "TMT", "m"),
    UGANDA_SHILLING("Ugandan Shilling", "UGX", "USh"),
    UKRAINIAN_HRYVNIA("Ukrainian Hryvnia", "UAH", "₴"),
    UNITED_ARAB_EMIRATES_DIRHAM("United Arab Emirates Dirham", "AED", "د.إ"),
    UNITED_STATES_DOLLAR("United States Dollar", "USD", "$"),
    URUGUAYAN_PESO("Uruguayan Peso", "UYU", "$"),
    UZBEKISTANI_SOM("Uzbekistani Som", "UZS", "лв."),
    VENEZUELAN_BOLIVAR("Venezuelan Bolívar", "VES", "Bs.S."),
    VIETNAMESE_DONG("Vietnamese Dong", "VND", "₫"),
    ZAMBIAN_KWACHA("Zambian Kwacha", "ZMW", "K"),
    ZIMBABWEAN_DOLLAR("Zimbabwean Dollar", "ZWL", "$");

    public static final Currency[] currencies;
    public final String NAME;
    public final String CODE;
    public final String SYMBOL;

    static {
        currencies = values();
    }

    Currency(String name, String code, String symbol) {
        this.NAME = name;
        this.CODE = code;
        this.SYMBOL = symbol;
    }

    public static boolean isRTLCurrency(String code) {
        String[] rtlCurrencies = {
                "ILS", "SAR", "AED", "EGP", "IRR", "IQD","JOD",
                "KWD", "LBP", "OMR", "QAR", "SYP", "YER", "PKR"
        };

        for (String rtlCurrency : rtlCurrencies) {
            if (rtlCurrency.equals(code)) {
                return true;
            }
        }
        return false;
    }

    public static String getName(String code) {
        for (Currency currency : currencies) {
            if (currency.CODE.equals(code)) {
                return currency.NAME;
            }
        }
        return null;
    }

    public static String getSymbol(String code) {
        for (Currency currency : currencies) {
            if (currency.CODE.equals(code)) {
                return currency.SYMBOL;
            }
        }
        return null;
    }

    public static String[] getNames() {
        String[] names = new String[values().length];
        for (int i = 0; i < values().length; i++) {
            names[i] = String.format("%s (%s)", values()[i].NAME, values()[i].SYMBOL);
        }
        return names;
    }

    public static String[] getCodes() {
        String[] codes = new String[values().length];
        for (int i = 0; i < values().length; i++) {
            codes[i] = values()[i].CODE;
        }
        return codes;
    }

    public static String formatAmount(String currencyCode, double amount) {
        String symbol = getSymbol(currencyCode);
        String formattedAmount = NumberFormat.getInstance().format(amount);

        String[] suffixCurrencies = {
                "EUR", "PLN", "CZK", "HUF", "SEK", "NOK", "DKK", "ISK", "TRY", "RON", "BGN", "HRK",
                "CHF", "RSD", "UAH", "BYN", "MDL", "GEL", "AMD", "AZN", "KZT", "KGS", "UZS", "TMT",
                "ALL", "BAM", "MKD", "RUB", "LVL", "LTL", "EEK", "SKK", "SIT",
                "BRL", "ARS", "CLP", "COP", "PEN", "UYU", "BOB", "PYG", "VES",
                "CNY", "JPY", "KRW", "VND", "THB", "MYR", "SGD", "IDR", "PHP", "LAK", "MMK", "KHR",
                "ZAR", "EGP", "MAD", "TND", "DZD", "NGN", "GHS", "KES", "TZS", "UGX", "RWF", "ETB",
                "TRY", "IRR", "IQD", "JOD", "LBP", "SYP", "QAR", "AED", "OMR", "BHD", "KWD", "SAR",
                "INR", "PKR", "BDT", "LKR", "NPR", "BTN", "AFN", "MZN", "AOA", "XAF", "XOF"
        };

        for (String suffixCurrency : suffixCurrencies) {
            if (suffixCurrency.equals(currencyCode)) {
                return formattedAmount + " " + symbol;
            }
        }

        return symbol + formattedAmount;
    }
}