package com.se1605.service.impl;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.exc.InvalidFormatException;
import com.fasterxml.jackson.dataformat.xml.XmlMapper;
import com.google.common.collect.Ordering;
import com.groupdocs.conversion.Converter;
import com.groupdocs.conversion.contracts.SavePageStream;
import com.groupdocs.conversion.licensing.License;
import com.groupdocs.conversion.options.convert.ConvertOptions;
import com.groupdocs.conversion.options.convert.ImageConvertOptions;
import com.se1605.config.ConversionConfiguration;
import com.se1605.config.DefaultDirectories;
import com.se1605.config.GlobalConfiguration;
import com.se1605.exception.TotalGroupDocsException;
import com.se1605.model.invoice.easyinvoice.Invoice;
import com.se1605.model.invoice.easyinvoice.Product;
import com.se1605.model.request.ConversionPostedData;
import com.se1605.model.request.FileTreeRequest;
import com.se1605.model.response.ConversionTypesEntity;
import com.se1605.service.ConversionService;
import com.se1605.util.DestinationTypesFilter;
import com.se1605.util.FileEncrypterDecrypter;
import jakarta.annotation.PostConstruct;
import org.apache.commons.io.FilenameUtils;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFCell;
import org.apache.poi.xssf.usermodel.XSSFRow;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.InputStreamResource;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.io.*;
import java.nio.file.Files;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import static com.se1605.util.Utils.*;

@Service
public class ConversionServiceImpl implements ConversionService {
    private static final Logger logger = LoggerFactory.getLogger(ConversionServiceImpl.class);
    @Autowired
    private ConversionConfiguration conversionConfiguration;
    @Autowired
    private GlobalConfiguration globalConfiguration;

    private List<String> supportedImageFormats = Arrays.asList("jp2", "ico", "psd", "svg", "bmp", "jpeg", "jpg", "tiff", "tif", "png", "gif", "emf", "wmf", "dwg", "dicom", "dxf", "jpe", "jfif");

    /**
     * Initializing fields after creating configuration objects
     */
    @PostConstruct
    public void init() {
        String filesDirectory = conversionConfiguration.getFilesDirectory();
        String resultDirectory = conversionConfiguration.getResultDirectory();
        DefaultDirectories.makeDirs(new File(resultDirectory));
        try {
            License license = new License();
            license.setLicense(globalConfiguration.getApplication().getLicensePath());
        } catch (Throwable exc) {
            logger.error("Can not verify Conversion license!");
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public List<ConversionTypesEntity> loadFiles(FileTreeRequest fileTreeRequest) {
        String currentPath = fileTreeRequest.getPath();
        if (StringUtils.isEmpty(currentPath)) {
            currentPath = conversionConfiguration.getFilesDirectory();
        } else {
            currentPath = String.format("%s%s%s", conversionConfiguration.getFilesDirectory(), File.separator, currentPath);
        }
        File directory = new File(currentPath);
        List<ConversionTypesEntity> fileList = new ArrayList<>();
        List<File> filesList = Arrays.asList(directory.listFiles());
        try {
            // sort list of files and folders
            filesList = Ordering.from(FILE_TYPE_COMPARATOR).compound(FILE_NAME_COMPARATOR).sortedCopy(filesList);
            for (File file : filesList) {
                // check if current file/folder is hidden
                if (file.isHidden()) {
                    // ignore current file and skip to next one
                    continue;
                } else {
                    ConversionTypesEntity fileDescription = getFileDescriptionEntity(file);
                    // add object to array list
                    fileList.add(fileDescription);
                }
            }
            return fileList;
        } catch (Exception ex) {
            logger.error("Exception occurred while load file tree");
            throw new TotalGroupDocsException(ex.getMessage(), ex);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public ConversionConfiguration getConversionConfiguration() {
        return conversionConfiguration;
    }

    public ResponseEntity download(String path) throws IOException {
        if (path != null && !path.isEmpty()) {

            String destinationPath = FilenameUtils.concat(conversionConfiguration.getResultDirectory(), path);
            String ext = FilenameUtils.getExtension(destinationPath);
            String fileNameWithoutExt = FilenameUtils.removeExtension(path);
            if (supportedImageFormats.contains(ext)) {
                String zipName = fileNameWithoutExt + ".zip";
                File zipPath = new File(FilenameUtils.concat(conversionConfiguration.getResultDirectory(), zipName));
                File[] files = new File(conversionConfiguration.getResultDirectory()).listFiles((d, name) ->
                        name.endsWith("." + ext) && name.startsWith(fileNameWithoutExt)
                );
                if (zipPath.exists()) {
                    zipPath.delete();
                }
                ZipOutputStream zipOut = new ZipOutputStream(new FileOutputStream(zipPath));
                for (File filePath : files) {
                    File fileToZip = filePath;
                    zipOut.putNextEntry(new ZipEntry(fileToZip.getName()));
                    Files.copy(fileToZip.toPath(), zipOut);
                }
                zipOut.close();
                destinationPath = zipPath.getAbsolutePath();
            }
            if (new File(destinationPath).exists()) {
                InputStreamResource content = new InputStreamResource(new FileInputStream(new File(destinationPath)));
                return ResponseEntity
                        .ok()
                        .contentType(MediaType.APPLICATION_OCTET_STREAM)
                        .header("Content-Disposition", "attachment; filename=" + FilenameUtils.getName(destinationPath))
                        .body(content);
            }
        }
        return ResponseEntity.notFound().build();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void convert(ConversionPostedData postedData, String key) {
        String sourceType = FilenameUtils.getExtension(postedData.getGuid());
        String destinationType = postedData.getDestinationType();
        String destinationFile = FilenameUtils.removeExtension(FilenameUtils.getName(postedData.getGuid())) + "." + destinationType;
        String resultFileName = FilenameUtils.concat(conversionConfiguration.getResultDirectory(), destinationFile);
        if (!(sourceType.equals("xml") && destinationType.equals("xlsx"))) {
            String srcFile = FilenameUtils.concat(conversionConfiguration.getFilesDirectory(), postedData.getGuid());
            FileEncrypterDecrypter fileEncrypterDecrypter = new FileEncrypterDecrypter(key);
            try {
                fileEncrypterDecrypter.decrypt(srcFile, FilenameUtils.removeExtension(resultFileName) + "." + sourceType);
            } catch (InvalidAlgorithmParameterException e) {
                throw new RuntimeException(e);
            } catch (InvalidKeyException e) {
                throw new RuntimeException(e);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
            Converter converter = new Converter(FilenameUtils.removeExtension(resultFileName) + "." + sourceType);
            ConvertOptions convertOptions = converter.getPossibleConversions().getTargetConversion(destinationType).getConvertOptions();
            if (convertOptions instanceof ImageConvertOptions) {
                converter.convert((SavePageStream) i -> {
                    try {
                        return new FileOutputStream(FilenameUtils.removeExtension(resultFileName) + "-page" + i + "." + destinationType);
                    } catch (FileNotFoundException e) {
                        throw new RuntimeException(e);
                    }
                }, convertOptions);
            } else {
                converter.convert(resultFileName, convertOptions);
            }
        } else {
            logger.info(FilenameUtils.getExtension(postedData.getGuid()));
            extractedFromXml2XlsxWithExistedTemplate(postedData, key, destinationType, resultFileName);
        }
    }

    private void extractedFromXml2XlsxWithExistedTemplate(ConversionPostedData postedData, String key, String destinationType, String resultFileName) {
        try {
            String srcFile = FilenameUtils.concat(conversionConfiguration.getFilesDirectory(), postedData.getGuid());
            FileEncrypterDecrypter fileEncrypterDecrypter = new FileEncrypterDecrypter(key);
            try {
                fileEncrypterDecrypter.decrypt(srcFile, FilenameUtils.removeExtension(resultFileName) + "." + destinationType);
            } catch (InvalidAlgorithmParameterException e) {
                throw new RuntimeException(e);
            } catch (InvalidKeyException e) {
                throw new RuntimeException(e);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
            XmlMapper xmlMapper = new XmlMapper();
            Invoice value = xmlMapper.readValue(new File(FilenameUtils.removeExtension(resultFileName) + "." + destinationType), Invoice.class);
            logger.warn(FilenameUtils.concat(conversionConfiguration.getFilesDirectory() + "/Conversion", "template.xlsx"));

            // Creating input stream
            Workbook workbook = new XSSFWorkbook(new File(FilenameUtils.concat(conversionConfiguration.getFilesDirectory() + "/Conversion", "template.xlsx")));
            logger.info(value.toString());
            // Get the sheet to work on
            Sheet sheet = workbook.getSheetAt(0);
            Product item = value.Content.Products.Product;

            XSSFRow rowCusName = (XSSFRow) sheet.getRow(8);
            XSSFCell cellCusName = rowCusName.getCell(0);
            cellCusName.setCellValue("Người nộp thuế: " + value.Content.CusName);
            XSSFRow rowCusTaxCode = (XSSFRow) sheet.getRow(9);
            XSSFCell cellCusTaxCode = rowCusTaxCode.getCell(0);
            cellCusTaxCode.setCellValue("Mã số thuế:: " + value.Content.CusTaxCode);


            XSSFRow row = (XSSFRow) sheet.getRow(18);
            XSSFCell cell = row.getCell(1);
            cell.setCellValue(1);


            XSSFCell cell2 = row.getCell(2);
            cell2.setCellValue(value.Content.SerialNo);


            XSSFCell cell3 = row.getCell(3);
            cell3.setCellValue(String.format("%07d", value.Content.InvoiceNo));


            XSSFCell cell4 = row.getCell(4);
            cell4.setCellValue(value.Content.ArisingDate);


            XSSFCell cell5 = row.getCell(5);
            cell5.setCellValue(value.Content.ComName);


            XSSFCell cell6 = row.getCell(6);
            cell6.setCellValue(value.Content.ComTaxCode);


            XSSFCell cell7 = row.getCell(7);
            cell7.setCellValue(String.valueOf(item.Code) != null ? "" : String.valueOf(item.Code));


            XSSFCell cell8 = row.getCell(8);
            cell8.setCellValue(item.ProdName);


            XSSFCell cell9 = row.getCell(9);
            cell9.setCellValue(value.Content.GrossValue5);


            XSSFCell cell10 = row.getCell(10);
            cell10.setCellValue(5);

            XSSFCell cell11 = row.getCell(11);
            cell11.setCellValue(value.Content.VatAmount5);

            XSSFCell cell12 = row.getCell(12);
            cell12.setCellValue("");

            XSSFCell cell13 = row.getCell(13);
            cell13.setCellValue(cell11.getNumericCellValue() + cell9.getNumericCellValue());

            XSSFRow rowTotal = (XSSFRow) sheet.getRow(23);
            XSSFCell cellGrossValue5 = rowTotal.getCell(9);
            cellGrossValue5.setCellValue(value.Content.GrossValue5);
            XSSFCell cellVatAmount5 = rowTotal.getCell(11);
            cellVatAmount5.setCellValue(value.Content.VatAmount5);

            FileOutputStream outputStream = new FileOutputStream(FilenameUtils.removeExtension(resultFileName) + "." + destinationType);
            workbook.write(outputStream);
            workbook.close();
            outputStream.close();
        } catch (InvalidFormatException | JsonParseException ex) {
            logger.error(ex.toString());
            throw new RuntimeException(ex);
        } catch (FileNotFoundException e) {
            logger.error(e.toString());
            throw new RuntimeException(e);
        } catch (Exception e) {
            logger.error(e.toString());
            throw new RuntimeException(e);
        }
    }


    /**
     * Create file description
     *
     * @param file file
     * @return file description
     */
    private ConversionTypesEntity getFileDescriptionEntity(File file) {
        ConversionTypesEntity fileDescription = new ConversionTypesEntity();
        // set path to file
        fileDescription.setGuid(file.getAbsolutePath());
        // set file name
        fileDescription.setName(file.getName());
        // set is directory true/false
        fileDescription.setDirectory(file.isDirectory());
        // set file size
        fileDescription.setSize(file.length());

        String ext = parseFileExtension(fileDescription.getGuid());
        if (ext != null && !ext.isEmpty()) {
            fileDescription.conversionTypes = new ArrayList<>();
            String[] availableTypes = new DestinationTypesFilter().getPosibleConversions(ext);
            for (String type : availableTypes) {
                fileDescription.conversionTypes.add(type);
            }
        }
        return fileDescription;
    }

}
