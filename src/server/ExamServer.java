package server;

import com.sun.net.httpserver.HttpExchange;
import entity.Appointment;
import entity.AppointmentManager;
import entity.Patient;
import freemarker.template.Configuration;
import freemarker.template.Template;
import freemarker.template.TemplateException;
import freemarker.template.TemplateExceptionHandler;
import utils.Generator;
import utils.Utils;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.*;
import java.util.stream.Collectors;

public class ExamServer extends BasicServer{

    private final static Configuration freemarker = initFreeMarker();
    private AppointmentManager appointmentsManager = new AppointmentManager();

    public ExamServer(String host, int port) throws IOException {
        super(host, port);
        initializeAppointments(appointmentsManager);
        registerGet("/appointments", this::appointmentsHandler);
        registerGet("/monthlyAppointments", this::monthlyAppointmentsHandler);
        registerGet("/appointments/day", this::dayAppointmentsHandler);
        registerPost("/appointments/add", this::addAppointmentHandler);
        registerPost("/appointments/delete", this::deleteAppointmentHandler);
        registerPost("/appointments/windowAdd", this::addAppointmentWindowHandler);
    }

    private void addAppointmentWindowHandler(HttpExchange exchange) {
        if ("POST".equalsIgnoreCase(exchange.getRequestMethod())) {
            try {
                String formData = new BufferedReader(
                        new InputStreamReader(exchange.getRequestBody(), StandardCharsets.UTF_8))
                        .lines().collect(Collectors.joining("\n"));

                Map<String, String> parsedFormData = Utils.parseUrlEncoded(formData, "&");

                LocalDate date = LocalDate.parse(parsedFormData.get("date"));
                LocalTime time = LocalTime.parse(parsedFormData.get("time"));
                LocalDateTime dateTime = LocalDateTime.of(date, time);


                if (dateTime.isBefore(LocalDateTime.now())) {
                    sendErrorResponse(exchange, "Нельзя записать на прошедшую дату.");
                    return;
                }

                String fullName = parsedFormData.get("fullName");
                LocalDate dob = LocalDate.parse(parsedFormData.get("dob"));
                String patientType = parsedFormData.get("patientType");
                String symptoms = parsedFormData.get("symptoms");

                Patient patient = new Patient(fullName, dob, patientType, symptoms);
                Appointment newAppointment = new Appointment(dateTime, patient);

                appointmentsManager.addAppointment(newAppointment);

                String redirectPath = "/appointments";
                redirect303(exchange, redirectPath);
            } catch (DateTimeParseException e) {
                e.printStackTrace();
            }
        }
    }


    private void sendErrorResponse(HttpExchange exchange, String errorMessage) {
        try{
            exchange.sendResponseHeaders(400, errorMessage.length());
            OutputStream os = exchange.getResponseBody();
            os.write(errorMessage.getBytes());
            os.close();
        } catch (IOException e){
            e.printStackTrace();
        }
    }


    private void deleteAppointmentHandler(HttpExchange exchange) {
        if ("POST".equals(exchange.getRequestMethod())) {
            try {
                InputStreamReader isr = new InputStreamReader
                        (exchange.getRequestBody(), StandardCharsets.UTF_8);
                BufferedReader br = new BufferedReader(isr);
                String formData = br.lines().collect(Collectors.joining("&"));
                Map<String, String> parsedFormData = Utils.parseUrlEncoded(formData, "&");

                String dateString = parsedFormData.get("date");

                UUID id = UUID.fromString(parsedFormData.get("appointmentId"));
                appointmentsManager.removeAppointment(id);

                String redirectPath = "/appointments/day?date=" + dateString;
                redirect303(exchange, redirectPath);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }


    private void addAppointmentHandler(HttpExchange exchange) {
        if ("POST".equals(exchange.getRequestMethod())) {
            try {
                InputStreamReader isr = new
                        InputStreamReader(exchange.getRequestBody(), StandardCharsets.UTF_8);
                BufferedReader br = new BufferedReader(isr);
                String formData = br.lines().collect(Collectors.joining("&"));
                Map<String, String> parsedFormData = Utils.parseUrlEncoded(formData, "&");

                LocalDate date = LocalDate.parse(parsedFormData.get("date"));
                LocalTime time = LocalTime.parse(parsedFormData.get("time"));
                LocalDateTime dateTime = LocalDateTime.of(date, time);
                String fullName = parsedFormData.get("fullName");
                String patientType = parsedFormData.get("patientType");
                String symptoms = parsedFormData.get("symptoms");

                Appointment newAppointment = new Appointment(dateTime, new Patient(fullName, patientType, symptoms));
                appointmentsManager.addAppointment(newAppointment);

                String redirectPath = "/appointments/day?date=" +
                        date.format(DateTimeFormatter.ofPattern("yyyy-MM-dd"));
                redirect303(exchange, redirectPath);
            } catch (DateTimeParseException e) {
                e.printStackTrace();
            }
        }
    }



    private void monthlyAppointmentsHandler(HttpExchange exchange) {
        List<Appointment> appointments = getAppointmentsForCurrentMonth();
        appointments.sort(Comparator.comparing(Appointment::getAppointmentTime));

        Map<String, Object> dataModel = new HashMap<>();
        Map<LocalDate, List<Appointment>> groupedAppointments = groupAppointmentsByDate(appointments);

        Map<String, List<Appointment>> stringKeyedAppointments = groupedAppointments.entrySet().stream()
                .collect(Collectors.toMap(
                        entry -> entry.getKey().format(DateTimeFormatter.ofPattern("yyyy-MM-dd")),
                        Map.Entry::getValue
                ));

        dataModel.put("groupedAppointments", stringKeyedAppointments);
        dataModel.put("today", LocalDate.now().format(DateTimeFormatter.ofPattern("dd-MM-yyyy")));
        renderTemplate(exchange, "data/monthlyAppointments.ftlh", dataModel);
    }

    private void dayAppointmentsHandler(HttpExchange exchange) {

        String query = exchange.getRequestURI().getQuery();
        Map<String, String> queryParams = Utils.parseUrlEncoded(query, "&");
        String selectedDateStr = queryParams.get("date");

        LocalDate selectedDate = LocalDate.parse(selectedDateStr, DateTimeFormatter.ofPattern("yyyy-MM-dd"));

        List<Appointment> appointmentsForDay = appointmentsManager.getAppointmentsForDate(selectedDate);

        formatDate(appointmentsForDay, "HH:mm");

        Map<String, Object> dataModel = new HashMap<>();
        dataModel.put("appointments", appointmentsForDay);
        dataModel.put("selectedDate", selectedDateStr);

        renderTemplate(exchange, "data/dayAppointments.ftlh", dataModel);
    }


    private List<Appointment> getAppointmentsForCurrentMonth() {
        LocalDate startOfMonth = LocalDate.now().withDayOfMonth(1);
        LocalDate endOfMonth = LocalDate.now().withDayOfMonth(LocalDate.now().lengthOfMonth());
        return appointmentsManager.getAllAppointments().stream()
                .filter(appointment -> !appointment.getAppointmentTime().toLocalDate().isBefore(startOfMonth) &&
                        !appointment.getAppointmentTime().toLocalDate().isAfter(endOfMonth))
                .collect(Collectors.toList());
    }

    private void appointmentsHandler(HttpExchange exchange) {
        List<Appointment> appointments = appointmentsManager.getAllAppointments();

        formatDate(appointments, "yyyy-MM-dd HH:mm");

        appointments.sort(Comparator.comparing(Appointment::getAppointmentTime));

        Map<String, Object> dataModel = new HashMap<>();
        dataModel.put("appointments", appointments);

        renderTemplate(exchange, "data/appointments.ftlh", dataModel);

    }

    private Map<LocalDate, List<Appointment>> groupAppointmentsByDate(List<Appointment> appointments) {
        return appointments.stream()
                .collect(Collectors.groupingBy(appointment -> appointment.getAppointmentTime().toLocalDate()));
    }

    protected final void registerPost(String route, RouteHandler handler) {
        getRoutes().put("POST " + route, handler);
    }


    private void formatDate(List<Appointment> appointments, String pattern) {
        for (Appointment appointment : appointments) {
            String formattedTime = appointment.getAppointmentTime()
                    .format(DateTimeFormatter.ofPattern(pattern));
            appointment.setFormattedTime(formattedTime);
        }
    }


    public static void initializeAppointments(AppointmentManager manager) {

        int numberOfAppointments = 10;
        Random random = new Random();

        for (int i = 0; i < numberOfAppointments; i++) {
            LocalDate date = LocalDate.now().plusDays(random.nextInt(30));
            LocalTime time = LocalTime.of(random.nextInt(10) + 8, 0);
            LocalDateTime dateTime = LocalDateTime.of(date, time);

            Patient patient = new Patient(
                    Generator.makeName(),
                    LocalDate.of(1980 + random.nextInt(40),
                            random.nextInt(12) + 1,
                            random.nextInt(28) + 1),
                    random.nextBoolean() ? "Первичный" : "Вторичный",
                    Generator.makeDescription()
            );
            Appointment appointment = new Appointment(dateTime, patient);
            manager.addAppointment(appointment);
        }
    }

    private static Configuration initFreeMarker() {
        Configuration cfg = new Configuration(Configuration.VERSION_2_3_29);
        try {
            cfg.setDirectoryForTemplateLoading(new File("."));
            cfg.setDefaultEncoding("UTF-8");
            cfg.setTemplateExceptionHandler(TemplateExceptionHandler.RETHROW_HANDLER);
            cfg.setLogTemplateExceptions(false);
            cfg.setWrapUncheckedExceptions(true);
            cfg.setFallbackOnNullLoopVariable(false);
        } catch (IOException e) {
            throw new RuntimeException("Failed to set directory for template loading", e);
        }
        return cfg;
    }

    protected void renderTemplate(HttpExchange exchange, String templateFile, Object dataModel) {
        try {
            Template template = freemarker.getTemplate(templateFile);
            ByteArrayOutputStream stream = new ByteArrayOutputStream();
            try (OutputStreamWriter writer = new OutputStreamWriter(stream)) {
                template.process(dataModel, writer);
                writer.flush();
                byte[] data = stream.toByteArray();
                sendByteData(exchange, ResponseCodes.OK, ContentType.TEXT_HTML, data);
            }
        } catch (IOException | TemplateException e) {
            e.printStackTrace();
            try {
                exchange.sendResponseHeaders(500, -1);
            } catch (IOException ex) {
                ex.printStackTrace();
            }
        }
    }
}
