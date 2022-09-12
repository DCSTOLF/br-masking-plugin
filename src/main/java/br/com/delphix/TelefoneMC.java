package br.com.delphix;

import com.delphix.masking.api.plugin.MaskingAlgorithm;
import com.delphix.masking.api.plugin.MaskingComponent;
import com.delphix.masking.api.plugin.exception.MaskingException;
import com.delphix.masking.api.plugin.exception.NonConformantDataException;
import com.delphix.masking.api.plugin.referenceType.AlgorithmInstanceReference;
import com.delphix.masking.api.plugin.utils.GenericData;
import com.delphix.masking.api.plugin.utils.GenericDataRow;
import com.delphix.masking.api.provider.ComponentService;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyDescription;
import com.google.common.base.Strings;

import java.lang.String;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Logger;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;


public class TelefoneMC implements MaskingAlgorithm<GenericDataRow> {

    private String name = "TelefoneMC";
    /**
     * Get the recommended name of this Algorithm.
     * @return The name of this algorithm
     */
    @Override
    public String getName() {
        // TODO: Change this if you'd like to name your algorithm differently from the Java class.
        return name;
    }

    public String getDescription() {
        return "Algoritmo para mascaramento CM:Numeric de Telefones armazenados  em 3 colunas:ddi, ddd, telefone";
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
        TelefoneMC instanceX = new TelefoneMC();
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

    public static final String TELEFONE_REGEX = "(?<!\\d|-|\\/|\\.)(\\+?55 ?)?\\(?(0?(?:1[1-9]|2[1,2,4,7,8]|3[1-5,7,8]|4[1-9]|5[1,3,4,5]|6[1-9]|7[1,3,4,5,7,9]|8[1-9]|9[1-9]))?\\)?[ -]?([2,3]\\d{3}-?\\d{4}|4[0-9][1-9]\\d-?\\d{4}|5[0-9]\\d{2}-?\\d{4}|9\\d{4}-?\\d{4}|7[0,7,8,9]\\d{2}-?\\d{4})(?!\\d|-|\\/|\\.)";

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

    public String replaceByMaskedNumbers(String originalValue, String maskedValue){
        int i=0;
        int m=0;
        StringBuilder result = new StringBuilder();

        while (m<maskedValue.length()){
            if (Character.isDigit(originalValue.charAt(i))){
                result.append(maskedValue.charAt(m));
                m++;
            } else {
                result.append(originalValue.charAt(i));
            }
            i++;

        }
        if (i<originalValue.length() && (! Character.isDigit(originalValue.charAt(i))) ){
            result.append(originalValue.charAt(i));
        }


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

    /**
     * Masks String object
     * @param genericDataRow The String object to be masked. This method should handle null inputs.
     * @return Returns the masked value.
     */
    @Override
    public GenericDataRow mask(@Nullable GenericDataRow genericDataRow) throws MaskingException {
        // read the column values from GenericDataRow
        GenericData countryDataRow = genericDataRow.get("codigoPais");
        GenericData areaDataRow = genericDataRow.get("codigoArea");
        GenericData phoneDataRow = genericDataRow.get("numeroTelefone");

        String countryCode = countryDataRow.getStringValue();
        String areaCode = areaDataRow.getStringValue();
        String phone = phoneDataRow.getStringValue();

        String maskedCountryCode="";
        String maskedAreaCode="";
        String maskedPhone="";

        if(!Strings.isNullOrEmpty(countryCode)) {
            maskedCountryCode=maskCountryCode(countryCode.replaceAll("[^\\d]", ""));
            countryDataRow.setValue( replaceByMaskedNumbers(countryCode, maskedCountryCode) );
        }
        if(!Strings.isNullOrEmpty(areaCode)) {
            maskedAreaCode=maskAreaCode(areaCode.replaceAll("[^\\d]", ""));
            areaDataRow.setValue( replaceByMaskedNumbers(areaCode, maskedAreaCode) );
        }
        if(!Strings.isNullOrEmpty(phone)) {
            maskedPhone=maskPhoneNumber(phone.replaceAll("[^\\d]", ""));
            phoneDataRow.setValue( replaceByMaskedNumbers(phone, maskedPhone) );
        }
        else
            throw new NonConformantDataException("WAR: Phone number is empty");

        return genericDataRow;
    }


    @Override
    public Map<String, MaskingType> listMaskedFields() {
        /*
         *  Here we define the column names to be used in the algorithm. These names are only used to reference the
         *  columns within the algorithm and do not need to correspond to the names of the columns on the data source.
         *  For example, our data source may call these 2 fields "dateOfBirth" and "dateOfDeath", however within the
         *  algorithm implementation they will be referenced as "startDate" and "endDate" (see mask method to see how
         *  this is used).
         */
        Map<String, MaskingType> maskedFields = new HashMap<String, MaskingType>();
        maskedFields.put("codigoPais", MaskingType.STRING);
        maskedFields.put("codigoArea", MaskingType.STRING);
        maskedFields.put("numeroTelefone", MaskingType.STRING);
        return maskedFields;
    }

}
