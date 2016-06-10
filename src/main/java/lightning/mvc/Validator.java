package lightning.mvc;

import java.io.IOException;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Pattern;

import javax.servlet.ServletException;
import javax.servlet.http.Part;

import com.google.common.base.Optional;

/**
 * Provides functionality for validating form fields.
 * 
 * Usage:
 *   Validator v = Validator.create(this);
 *   v.check("field").isEmail();
 */
public class Validator {
  private final HandlerContext controller;
  private final Map<String, String> errors;
  
  public Validator(HandlerContext controller) {
    this.controller = controller;
    errors = new HashMap<>();
  }
  
  public FieldValidator check(String field) {
    return new FieldValidator(field);
  }
  
  public void addError(String field, String message) {
    errors.put(field, message);
  }
  
  public Map<String, String> getErrors() {
    return errors;
  }
  
  public String getErrorsAsString() {
    StringBuilder s = new StringBuilder();
    
    for (Map.Entry<String, String> e : getErrors().entrySet()) {
      s.append(String.format("%s: %s\n", e.getKey(), e.getValue()));
    }
    
    return s.toString();
  }

  public Optional<String> getErrorOption(String field) {
    return Optional.fromNullable(errors.get(field));
  }
  
  public boolean hasErrors() {
    return !errors.isEmpty();
  }
  
  public boolean passes() {
    return errors.isEmpty();
  }
  
  public static Validator create(HandlerContext controller) {
    return new Validator(controller);
  }
  
  @FunctionalInterface
  public static interface CustomValidator {
    public boolean check(Param value);
  }
  
  public final class FieldValidator {
    private final Param field;
    
    public FieldValidator(String field) {
      this.field = controller.request.queryParam(field);
    }
    
    public FieldValidator addError(String message) {
      Validator.this.addError(field.getKey(), message);
      return this;
    }
    
    public FieldValidator isEmail() {
      if (!field.isEmail()) {
        addError("You must enter a valid email address.");
      }
      
      return this;
    }
    
    public FieldValidator isURL() {
      if (!field.isURL()) {
        addError("You must enter a valid URL.");
      }
      
      return this;
    }
    
    public FieldValidator isChecked() {
      if (!field.isChecked()) {
        addError("This field must be checked.");
      }
      
      return this;
    }
    
    public FieldValidator isPresent() {
      if (!field.isNotNull()) {
        addError("This field is required.");
      }
      
      return this;
    }
    
    public FieldValidator is(String requiredValue, String errorMessage) {
      if (!field.isEqualTo(requiredValue)) {
        addError(errorMessage);
      }
      
      return this;
    }
    
    public FieldValidator is(String requiredValue) {
      if (!field.isEqualTo(requiredValue)) {
        addError("You must enter the required value to proceed.");
      }
      
      return this;
    }
    
    public FieldValidator isEqualToCaseInsensitive(String requiredValue) {
      if (!field.isEqualToCaseInsensitive(requiredValue)) {
        addError("You must enter the required value to proceed.");
      }
      
      return this;
    }
    
    public FieldValidator isNotChecked() {
      if (!field.isNotChecked()) {
        addError("This field must not be checked.");
      }
      
      return this;
    }   

    public FieldValidator isShorterThan(long maxStringLength) {
      if (!field.isShorterThan(maxStringLength)) {
        addError("This field must be at most " + maxStringLength + " characters long.");
      }
      
      return this;
    }
    
    public FieldValidator isLongerThan(long minStringLength) {
      if (!field.isLongerThan(minStringLength)) {
        addError("This field must be at least " + minStringLength + " characters long.");
      }
        
      return this;
    }
    
    public FieldValidator isNotEmpty() {
      if (!field.isNotEmpty()) {
        addError("You must provide a non-empty value.");
      }
      
      return this;
    }

    public FieldValidator isAlphaNumeric() {
      if (!field.isAlphaNumeric()) {
        addError("You must enter an alphanumeric value.");
      }
      
      return this;
    }
    
    public FieldValidator isAlpha() {
      if (!field.isAlpha()) {
        addError("You must enter an alphabetical value.");
      }
      
      return this;
    }
    
    public FieldValidator isAlphaNumericWithSpaces() {
      if (!field.isAlphaNumericWithSpaces()) {
        addError("You must enter an alphanumeric value.");
      }
      
      return this;
    }
    
    public FieldValidator isAlphaWithSpaces() {
      if (!field.isAlphaWithSpaces()) {
        addError("You must enter an alphabetical value.");
      }
      
      return this;
    }
    
    public FieldValidator isAlphaNumericDashUnderscore() {
      if (!field.isAlphaNumericDashUnderscore()) {
        addError("You must enter a value containing only alphanumeric characters, dashes, and underscores.");
      }
      
      return this;
    }
    
    public FieldValidator matches(CustomValidator validator, String errorMessage) {
      if (field.isEmpty()) {
        addError("You must provide a non-empty value.");
        return this;
      }
      
      if(!validator.check(field)) {
        addError(errorMessage);
      }
      
      return this;
    }
    
    public FieldValidator matches(String regex, String errorMessage) {
      if (!field.matches(regex)) {
        addError(errorMessage);
      }
      
      return this;
    }
    
    public FieldValidator matches(Pattern pattern, String errorMessage) {
      if (!field.matches(pattern)) {
        addError(errorMessage);
      }
      
      return this;
    }
        
    public FieldValidator isLong() {
      if (!field.isLong()) {
        addError("You must enter a valid integer.");
      }
      
      return this;
    }
    
    public FieldValidator isDouble() {
      if (!field.isDouble()) {
        addError("You must enter a valid decimal number.");
      }
      
      return this;
    }
    
    public FieldValidator isOneOf(Iterable<String> values) {
      if (!field.isOneOf(values)) {
        addError("You must enter an accepted value.");
      }
      
      return this;
    }
    
    public FieldValidator containsOnly(Iterable<String> values) {
      if (!field.containsOnly(values)) {
        addError("You must enter an accepted value.");
      }
      
      return this;
    }
    
    public FieldValidator hasNoDuplicates() {
      if (!field.hasNoDuplicates()) {
        addError("You must enter an accepted value.");
      }
      
      return this;
    }

    public FieldValidator isNonEmptyFile() throws IOException, ServletException {
      if (!controller.isMultipart()) {
        addError("You must upload a non-empty file.");
        return this;
      }
      
      Part p = controller.request.raw().getPart(field.getKey());
      
      if (p == null || p.getSize() == 0) {
        addError("You must upload a non-empty file.");
        return this;
      }
      
      return this;
    }
    
    public FieldValidator isFileOfType(Collection<String> mimeTypes) throws IOException, ServletException {
      if (!controller.isMultipart()) {
        addError("You must upload a file.");
        return this;
      }
      
      Part p = controller.request.raw().getPart(field.getKey());
      
      if (p == null) {
        addError("You must upload a file.");
        return this;
      }
      
      String type = p.getContentType();
      
      if (type.contains(";")) {
        type = type.substring(0, type.indexOf(";"));
      }
      
      if (!mimeTypes.contains(type)) {
        addError("You must upload a file of the required type.");
        return this;
      }
      
      return this;
    }
    
    public FieldValidator isFileWithExtension(Collection<String> extensions) throws IOException, ServletException {
      if (!controller.isMultipart()) {
        addError("You must upload a file.");
        return this;
      }
      
      Part p = controller.request.raw().getPart(field.getKey());
      
      if (p == null) {
        addError("You must upload a file.");
        return this;
      }
      
      String name = p.getSubmittedFileName();
      
      if (name == null) {
        addError("You must upload a file.");
        return this;
      }
      
      String extension = name.lastIndexOf(".") == -1 ? "" : name.substring(name.lastIndexOf(".") + 1, name.length());
      
      if (!extensions.contains(extension)) {
        addError("You must upload a file of the required type.");
        return this;
      }
      
      return this;
    }

    public FieldValidator isFile() throws IOException, ServletException {
      if (!controller.isMultipart()) {
        addError("You must upload a file.");
        return this;
      }
      
      Part p = controller.request.raw().getPart(field.getKey());
      
      if (p == null) {
        addError("You must upload a file.");
        return this;
      }
      
      String name = p.getSubmittedFileName();
      
      if (name == null) {
        addError("You must upload a file.");
        return this;
      }
      
      return this;
    }
    
    public FieldValidator isFileSmallerThan(long bytes) throws IOException, ServletException {
      if (!controller.isMultipart()) {
        addError("You must upload a file.");
        return this;
      }
      
      Part p = controller.request.raw().getPart(field.getKey());
      
      if (p == null) {
        addError("You must upload a file.");
        return this;
      }
      
      String name = p.getSubmittedFileName();
      
      if (name == null) {
        addError("You must upload a file.");
        return this;
      }
      
      if (p.getSize() > bytes) {
        addError("You must upload a file smaller than " + bytes + " bytes.");
        return this;
      }
      
      return this;
    }

    public FieldValidator isPositiveNumber() {
      if (!field.isPositive()) {
        addError("You must enter a positive number.");
      }
      
      return this;
    }
    
    public FieldValidator isNumberAtLeast(long n) {
      if (!field.isAtLeast(n)) {
        addError("You must enter number less than or equal to " + n + ".");
      }
      
      return this;
    }
    
    public FieldValidator isNumberAtMost(long n) {
      if (!field.isAtMost(n)) {
        addError("You must enter number less than or equal to " + n + ".");
      }
      
      return this;
    }
    
    public FieldValidator isNumberInRange(long min, long max) {
      if (!field.isInRange(min, max)) {
        addError("You must enter number between " + min + " and " + max + ".");
      }
      
      return this;
    }
    
    public FieldValidator isPositiveNonZeroNumber() {
      if (!field.isPositiveNonZero()) {
        addError("You must enter a positive non-zero number.");
      }
      
      return this;
    }
        
    public FieldValidator isNumberAtMost(double n) {
      if (!field.isAtMost(n)) {
        addError("You must enter number less than or equal to " + n + ".");
      }
      
      return this;
    }
    
    public FieldValidator isNumberAtLeast(double n) {
      if (!field.isAtLeast(n)) {
        addError("You must enter number less than or equal to " + n + ".");
      }
      
      return this;
    }
    
    public FieldValidator isNumberInRange(double min, double max) {
      if (!field.isInRange(min, max)) {
        addError("You must enter number between " + min + " and " + max + ".");
      }
      
      return this;
    }
  
    public <T extends Enum<T>> FieldValidator isEnum(Class<T> type) {
      if (!field.isEnum(type)) {
        addError("Error: You must enter a value of type " + type.getSimpleName());
      }
      
      return this;
    }
  }
}
