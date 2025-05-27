package servlet; // o el paquete que prefieras para tus servlets

import dao.ClienteJpaController;
import dao.exceptions.NonexistentEntityException;
import dto.Cliente;

import javax.persistence.EntityManagerFactory;
import javax.persistence.Persistence; // Para JPA estándar
// Para Java EE @PersistenceUnit:
// import javax.persistence.PersistenceUnit; 

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringReader;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.json.Json;
import javax.json.JsonArray;
import javax.json.JsonArrayBuilder;
import javax.json.JsonObject;
import javax.json.JsonObjectBuilder;
import javax.json.JsonReader;
import javax.json.JsonWriter;
import javax.json.JsonException;


@WebServlet(name = "ClienteServlet", urlPatterns = {"/ClienteServlet"})
public class ClienteServlet extends HttpServlet {

    // MUY IMPORTANTE: Reemplaza "miUnidadPersistenciaPU" con el nombre 
    // de tu unidad de persistencia definida en persistence.xml
    private static final String PERSISTENCE_UNIT_NAME = "com.mycompany_Fact_war_1.0-SNAPSHOTPU"; 
    private EntityManagerFactory emf;
    private ClienteJpaController clienteDao;

    @Override
    public void init() throws ServletException {
        super.init();
        try {
            // Usar Persistence.createEntityManagerFactory para entornos SE o si @PersistenceUnit no funciona
            emf = Persistence.createEntityManagerFactory(PERSISTENCE_UNIT_NAME);
            // Si estás en un contenedor Java EE completo, podrías inyectar con @PersistenceUnit
            // @PersistenceUnit(unitName = PERSISTENCE_UNIT_NAME)
            // private EntityManagerFactory emf;
            // y entonces no necesitarías Persistence.createEntityManagerFactory()
            clienteDao = new ClienteJpaController(emf);
        } catch (Exception e) {
            throw new ServletException("Error inicializando EntityManagerFactory o DAO", e);
        }
    }

    private JsonObject clienteToJson(Cliente cliente) {
        JsonObjectBuilder builder = Json.createObjectBuilder();
        if (cliente.getCodiClie() != null) {
            builder.add("codiClie", cliente.getCodiClie());
        }
        if (cliente.getNdniClie() != null) {
            builder.add("ndniClie", cliente.getNdniClie());
        } else {
            builder.addNull("ndniClie");
        }
        if (cliente.getAppaClie() != null) {
            builder.add("appaClie", cliente.getAppaClie());
        } else {
            builder.addNull("appaClie");
        }
        if (cliente.getApmaClie() != null) {
            builder.add("apmaClie", cliente.getApmaClie());
        } else {
            builder.addNull("apmaClie");
        }
        if (cliente.getNombClie() != null) {
            builder.add("nombClie", cliente.getNombClie());
        } else {
            builder.addNull("nombClie");
        }
        if (cliente.getFechNaciClie() != null) {
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
            builder.add("fechNaciClie", sdf.format(cliente.getFechNaciClie()));
        } else {
            builder.addNull("fechNaciClie");
        }
        if (cliente.getLogiClie() != null) {
            builder.add("logiClie", cliente.getLogiClie());
        } else {
            builder.addNull("logiClie");
        }
        // Por seguridad, usualmente no se envía la contraseña al cliente.
        // Si necesitas enviarla, descomenta la siguiente línea.
        // if (cliente.getPassClie() != null) {
        //     builder.add("passClie", cliente.getPassClie());
        // } else {
        //     builder.addNull("passClie");
        // }
        return builder.build();
    }

    private JsonArray clientesToJson(List<Cliente> clientes) {
        JsonArrayBuilder arrayBuilder = Json.createArrayBuilder();
        for (Cliente cliente : clientes) {
            arrayBuilder.add(clienteToJson(cliente));
        }
        return arrayBuilder.build();
    }
    
    private Cliente jsonToCliente(JsonObject json) throws ParseException {
        Cliente cliente = new Cliente();
        if (json.containsKey("codiClie") && !json.isNull("codiClie")) {
            cliente.setCodiClie(json.getInt("codiClie"));
        }
        if (json.containsKey("ndniClie") && !json.isNull("ndniClie")) {
            cliente.setNdniClie(json.getString("ndniClie"));
        }
        if (json.containsKey("appaClie") && !json.isNull("appaClie")) {
            cliente.setAppaClie(json.getString("appaClie"));
        }
        if (json.containsKey("apmaClie") && !json.isNull("apmaClie")) {
            cliente.setApmaClie(json.getString("apmaClie"));
        }
        if (json.containsKey("nombClie") && !json.isNull("nombClie")) {
            cliente.setNombClie(json.getString("nombClie"));
        }
        if (json.containsKey("fechNaciClie") && !json.isNull("fechNaciClie")) {
            String dateStr = json.getString("fechNaciClie");
            if (dateStr != null && !dateStr.trim().isEmpty()) {
                SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
                cliente.setFechNaciClie(sdf.parse(dateStr));
            }
        }
        if (json.containsKey("logiClie") && !json.isNull("logiClie")) {
            cliente.setLogiClie(json.getString("logiClie"));
        }
        if (json.containsKey("passClie") && !json.isNull("passClie")) {
            cliente.setPassClie(json.getString("passClie"));
        }
        return cliente;
    }

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        request.setCharacterEncoding("UTF-8");
        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");

        String idParam = request.getParameter("codiClie");
        PrintWriter out = response.getWriter();
        JsonWriter jsonWriter = Json.createWriter(out);

        try {
            if (idParam != null && !idParam.isEmpty()) {
                Integer id = Integer.parseInt(idParam);
                Cliente cliente = clienteDao.findCliente(id);
                if (cliente != null) {
                    jsonWriter.writeObject(clienteToJson(cliente));
                } else {
                    response.setStatus(HttpServletResponse.SC_NOT_FOUND);
                    JsonObject errorJson = Json.createObjectBuilder()
                                             .add("error", "Cliente no encontrado con ID: " + id)
                                             .build();
                    jsonWriter.writeObject(errorJson);
                }
            } else {
                List<Cliente> clientes = clienteDao.findClienteEntities();
                jsonWriter.writeArray(clientesToJson(clientes));
            }
        } catch (NumberFormatException e) {
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            JsonObject errorJson = Json.createObjectBuilder()
                                     .add("error", "Parámetro 'codiClie' inválido.")
                                     .build();
            jsonWriter.writeObject(errorJson);
        } catch (JsonException e) {
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            JsonObject errorJson = Json.createObjectBuilder()
                                     .add("error", "Error generando JSON: " + e.getMessage())
                                     .build();
            jsonWriter.writeObject(errorJson);
            Logger.getLogger(ClienteServlet.class.getName()).log(Level.SEVERE, "Error generando JSON", e);
        } finally {
            if (jsonWriter != null) {
                jsonWriter.close();
            }
            if (out != null) {
                out.close();
            }
        }
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        request.setCharacterEncoding("UTF-8");
        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");
        
        PrintWriter out = response.getWriter();
        JsonWriter jsonWriter = Json.createWriter(out);

        try (JsonReader jsonReader = Json.createReader(request.getReader())) {
            JsonObject jsonCliente = jsonReader.readObject();
            Cliente cliente = jsonToCliente(jsonCliente);
            
            // For POST, we usually don't expect an ID, or it should be null
            // as it's auto-generated.
            cliente.setCodiClie(null); 

            clienteDao.create(cliente);
            response.setStatus(HttpServletResponse.SC_CREATED);
            jsonWriter.writeObject(clienteToJson(cliente)); // Devuelve el cliente creado con su ID

        } catch (JsonException e) {
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST); // Malformed JSON
            JsonObject errorJson = Json.createObjectBuilder()
                                     .add("error", "JSON inválido o malformado: " + e.getMessage())
                                     .build();
            jsonWriter.writeObject(errorJson);
            Logger.getLogger(ClienteServlet.class.getName()).log(Level.WARNING, "JSON inválido en POST", e);
        } catch (ParseException e) {
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            JsonObject errorJson = Json.createObjectBuilder()
                                     .add("error", "Formato de fecha inválido. Usar yyyy-MM-dd. " + e.getMessage())
                                     .build();
            jsonWriter.writeObject(errorJson);
        } catch (Exception e) {
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            JsonObject errorJson = Json.createObjectBuilder()
                                     .add("error", "Error procesando la solicitud POST: " + e.getMessage())
                                     .build();
            jsonWriter.writeObject(errorJson);
            Logger.getLogger(ClienteServlet.class.getName()).log(Level.SEVERE, "Error en doPost", e);
        } finally {
            if (jsonWriter != null) {
                jsonWriter.close();
            }
            if (out != null) {
                out.close();
            }
        }
    }

    @Override
    protected void doPut(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        request.setCharacterEncoding("UTF-8");
        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");

        PrintWriter out = response.getWriter();
        JsonWriter jsonWriter = Json.createWriter(out);

        try (JsonReader jsonReader = Json.createReader(request.getReader())) {
            JsonObject jsonCliente = jsonReader.readObject();
            Cliente cliente = jsonToCliente(jsonCliente);

            if (cliente.getCodiClie() == null) {
                response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                JsonObject errorJson = Json.createObjectBuilder()
                                         .add("error", "El campo 'codiClie' es requerido para actualizar.")
                                         .build();
                jsonWriter.writeObject(errorJson);
                return;
            }
            
            // Verificar si el cliente existe antes de intentar editar
            if (clienteDao.findCliente(cliente.getCodiClie()) == null) {
                response.setStatus(HttpServletResponse.SC_NOT_FOUND);
                JsonObject errorJson = Json.createObjectBuilder()
                                         .add("error", "Cliente con ID " + cliente.getCodiClie() + " no encontrado para actualizar.")
                                         .build();
                jsonWriter.writeObject(errorJson);
                return;
            }

            clienteDao.edit(cliente);
            jsonWriter.writeObject(clienteToJson(cliente)); // Devuelve el cliente actualizado

        } catch (JsonException e) {
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST); // Malformed JSON
            JsonObject errorJson = Json.createObjectBuilder()
                                     .add("error", "JSON inválido o malformado: " + e.getMessage())
                                     .build();
            jsonWriter.writeObject(errorJson);
            Logger.getLogger(ClienteServlet.class.getName()).log(Level.WARNING, "JSON inválido en PUT", e);
        } catch (ParseException e) {
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            JsonObject errorJson = Json.createObjectBuilder()
                                     .add("error", "Formato de fecha inválido. Usar yyyy-MM-dd. " + e.getMessage())
                                     .build();
            jsonWriter.writeObject(errorJson);
        } catch (NonexistentEntityException e) {
            response.setStatus(HttpServletResponse.SC_NOT_FOUND);
             JsonObject errorJson = Json.createObjectBuilder()
                                     .add("error", "Cliente no encontrado para actualizar: " + e.getMessage())
                                     .build();
            jsonWriter.writeObject(errorJson);
        } catch (Exception e) {
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            JsonObject errorJson = Json.createObjectBuilder()
                                     .add("error", "Error procesando la solicitud PUT: " + e.getMessage())
                                     .build();
            jsonWriter.writeObject(errorJson);
            Logger.getLogger(ClienteServlet.class.getName()).log(Level.SEVERE, "Error en doPut", e);
        } finally {
            if (jsonWriter != null) {
                jsonWriter.close();
            }
            if (out != null) {
                out.close();
            }
        }
    }

    @Override
    protected void doDelete(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        request.setCharacterEncoding("UTF-8");
        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");

        String idParam = request.getParameter("codiClie");
        PrintWriter out = response.getWriter();
        JsonWriter jsonWriter = Json.createWriter(out);

        if (idParam == null || idParam.isEmpty()) {
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            JsonObject errorJson = Json.createObjectBuilder()
                                     .add("error", "Parámetro 'codiClie' es requerido para eliminar.")
                                     .build();
            jsonWriter.writeObject(errorJson);
            jsonWriter.close();
            out.close();
            return;
        }

        try {
            Integer id = Integer.parseInt(idParam);
            
            // Verificar si el cliente existe antes de intentar eliminar
            if (clienteDao.findCliente(id) == null) {
                response.setStatus(HttpServletResponse.SC_NOT_FOUND);
                JsonObject errorJson = Json.createObjectBuilder()
                                         .add("error", "Cliente con ID " + id + " no encontrado para eliminar.")
                                         .build();
                jsonWriter.writeObject(errorJson);
                return;
            }
            
            clienteDao.destroy(id);
            response.setStatus(HttpServletResponse.SC_OK); // O SC_NO_CONTENT si no devuelves nada
            JsonObject successJson = Json.createObjectBuilder()
                                     .add("message", "Cliente con ID " + id + " eliminado correctamente.")
                                     .build();
            jsonWriter.writeObject(successJson);

        } catch (NumberFormatException e) {
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            JsonObject errorJson = Json.createObjectBuilder()
                                     .add("error", "Parámetro 'codiClie' inválido.")
                                     .build();
            jsonWriter.writeObject(errorJson);
        } catch (NonexistentEntityException e) {
            response.setStatus(HttpServletResponse.SC_NOT_FOUND);
            JsonObject errorJson = Json.createObjectBuilder()
                                     .add("error", "Cliente no encontrado para eliminar: " + e.getMessage())
                                     .build();
            jsonWriter.writeObject(errorJson);
        } catch (Exception e) {
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            JsonObject errorJson = Json.createObjectBuilder()
                                     .add("error", "Error procesando la solicitud DELETE: " + e.getMessage())
                                     .build();
            jsonWriter.writeObject(errorJson);
            Logger.getLogger(ClienteServlet.class.getName()).log(Level.SEVERE, "Error en doDelete", e);
        } finally {
            if (jsonWriter != null) {
                jsonWriter.close();
            }
            if (out != null) {
                out.close();
            }
        }
    }

    @Override
    public String getServletInfo() {
        return "Servlet para gestionar Clientes con JSON";
    }

    @Override
    public void destroy() {
        if (emf != null && emf.isOpen()) {
            emf.close();
        }
        super.destroy();
    }
}