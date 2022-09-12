package br.com.delphix;

import com.delphix.masking.api.plugin.MaskingAlgorithm;
import com.delphix.masking.api.plugin.MaskingComponent;
import com.delphix.masking.api.plugin.exception.MaskingException;
import com.delphix.masking.api.plugin.exception.NonConformantDataException;
import com.delphix.masking.api.plugin.referenceType.AlgorithmInstanceReference;
import com.delphix.masking.api.provider.ComponentService;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyDescription;

import com.google.common.base.Strings;
import java.lang.String;
import java.lang.reflect.Array;
import java.util.Arrays;
import java.util.Collection;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.logging.Logger;

public class Telefone implements MaskingAlgorithm<String> {

    private String name = "Telefone";

    /**
     * Get the recommended name of this Algorithm.
     * @return The name of this algorithm
     */
    @Override
    public String getName() {
        return name;
    }

    public String getDescription() {
        return "Algoritmo para mascaramento CM:Numeric de Telefones armazenados em colunas únicas";
    }

    @Override
    public boolean getAllowFurtherInstances() {
        return true;
    }

    @JsonProperty("chain_algo_ref")
    @JsonPropertyDescription("Reference to AlgorithmInstanceReference of type string > SL ")
    public AlgorithmInstanceReference chain_algo_ref;
    private MaskingAlgorithm<String> chained_algo;

    @JsonProperty("mask_invalid")
    @JsonPropertyDescription("Flag to mask or not invalid numbers. Default False")
    public boolean mask_invalid;

    @Override
    public Collection<MaskingComponent> getDefaultInstances() {
        Telefone instanceX = new Telefone();
        instanceX.name = name;
        instanceX.mask_invalid = false;
        instanceX.chain_algo_ref = new AlgorithmInstanceReference("dlpx-core:CM Numeric");
        return Arrays.asList(instanceX);
    }

    @Override
    public void setup(@Nonnull ComponentService serviceProvider) {
        try {
            chained_algo = serviceProvider.getAlgorithmByName(chain_algo_ref, MaskingType.STRING);
        } catch (Exception e){
            e.printStackTrace();
        }
    }


    private static final Logger LOGGER = Logger.getLogger("InfoLogging");

    private static final int[][] VALID_LOCAL_CODES = {
            {},
            {1,2,3,4,5,6,7,8,9},
            {1,2,4,7,8},
            {1,2,3,4,5,7,8},
            {1,2,3,4,5,6,7,8,9},
            {1,3,4,5},
            {1,2,3,4,5,6,7,8,9},
            {1,3,4,5,7,9},
            {1,2,3,4,5,6,7,8,9},
            {1,2,3,4,5,6,7,8,9},
            {1,2,3,4,5,6,7,8,9}
    };
    public static final String TELEFONE_REGEX = "^\\s*(?<!\\d|-|\\/|\\.)(\\+?55 ?)?\\(?(0?(?:1[1-9]|2[1,2,4,7,8]|3[1-5,7,8]|4[1-9]|5[1,3,4,5]|6[1-9]|7[1,3,4,5,7,9]|8[1-9]|9[1-9]))?\\)?[ -]?([2,3]\\d{3}-?\\d{4}|4[0-9][1-9]\\d-?\\d{4}|5[0-9]\\d{2}-?\\d{4}|9\\d{4}-?\\d{4}|7[0,7,8,9]\\d{2}-?\\d{4})(?!\\d|-|\\/|\\.)\\s*$";


    private static boolean empty(final String s) {
        // check for null or empty string
        return s == null || s.trim().isEmpty();
    }

    public String maskCountryCode (@Nullable String input) throws MaskingException  {
        return input;
    }

    public String maskAreaCode(@Nullable String input) throws MaskingException  {
        if (empty(input)) {
            return input;
        }
        String maskedCode;
        int i=0;
        StringBuilder result = new StringBuilder();
        if (input.length()==3 || input.charAt(i)==0){
            result.append(input.charAt(i));
            i++;
        }
        maskedCode = chained_algo.mask(input);
        int firstDigit=Character.getNumericValue(maskedCode.charAt(i));
        int[] tmp = VALID_LOCAL_CODES[firstDigit];
        int secondDigit=tmp[(tmp.length-1)%firstDigit];
        result.append(firstDigit);
        result.append(secondDigit);
        return result.toString();
    }

    public String maskPhoneNumber(@Nullable String input) throws MaskingException  {
        if (empty(input)) {
            throw new NonConformantDataException("WAR: Telephone number is null");
        }
        StringBuilder result = new StringBuilder();
        int s=0;
        if ((input.matches("[2,3]\\d{3}-?\\d{4}"))){
            result.append(input.substring(0,1));
            s=1;
        } else if ((input.matches("4\\d{3}-?\\d{4}"))){
            result.append(input.substring(0,2));
            s=2;
        } else if ((input.matches("5\\d{3}-?\\d{4}"))){
            result.append(input.substring(0,1));
            s=1;
        } else if ((input.matches("9\\d{4}-?\\d{4}"))){
            result.append(input.substring(0,1));
            s=1;
        } else if ((input.matches("7[0,7,8,9]\\d{2}-?\\d{4}"))){
            result.append(input.substring(0,2));
            s=2;
        }

        String maskedNumber = chained_algo.mask(input.substring(s, input.length()));
        result.append(maskedNumber);
        return result.toString();
    }

    public String maskValue(@Nullable String fullNumber, @Nullable String countryCode, @Nullable String areaCode, String phone) throws MaskingException  {
        StringBuilder n = new StringBuilder();

        if(!Strings.isNullOrEmpty(countryCode))
            n.append( maskCountryCode(countryCode.replaceAll("[^\\d]", "")));
        if(!Strings.isNullOrEmpty(areaCode))
            n.append( maskAreaCode(areaCode.replaceAll("[^\\d]", "")));
        if(!Strings.isNullOrEmpty(phone))
            n.append( maskPhoneNumber(phone.replaceAll("[^\\d]", "")));
        else
            throw new NonConformantDataException("WAR: Phone number is empty");

        String maskedNumbers=n.toString();
        StringBuilder result = new StringBuilder();
        int i =0;
        int m = 0;
        while (m<maskedNumbers.length()){
            if (Character.isDigit(fullNumber.charAt(i))){
                result.append(maskedNumbers.charAt(m));
                m++;
            } else {
                result.append(fullNumber.charAt(i));
            }
            i++;

        }
        if (i<fullNumber.length() && (! Character.isDigit(fullNumber.charAt(i))) ){
            result.append(fullNumber.charAt(i));
        }

        return result.toString();
    }
    /**
     * Masks String object
     * @param input The String object to be masked. This method should handle null inputs.
     * @return Returns the masked value.
     */
    @Override
    public String mask(@Nullable String input) throws MaskingException {
        if (empty(input)) {
            return input;
        }

        if (!(input.matches(TELEFONE_REGEX))) {
            throw new NonConformantDataException("WAR: Input doesn't match a telephone regex");
        }

        Pattern p = Pattern.compile(TELEFONE_REGEX);
        Matcher m = p.matcher(input);
        StringBuffer result = new StringBuffer();
        while (m.find()) {
            m.appendReplacement(result, maskValue(m.group(0), m.group(1), m.group(2),m.group(3)));
        }
        m.appendTail(result);
        return result.toString();
    }
}
