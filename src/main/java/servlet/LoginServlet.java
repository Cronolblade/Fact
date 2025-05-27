package servlet;

import dao.ClienteJpaController;
import dto.Cliente;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter; // Para construir el JSON
import javax.json.Json; // API javax.json
import javax.json.JsonObject;
import javax.json.JsonObjectBuilder;
import javax.json.JsonWriter;
import javax.persistence.EntityManagerFactory;
import javax.persistence.Persistence;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

@WebServlet(name = "LoginServlet", urlPatterns = {"/LoginServlet"})
public class LoginServlet extends HttpServlet {

    private EntityManagerFactory emf;
    private ClienteJpaController clienteController;

    @Override
    public void init() throws ServletException {
        super.init();
        // Reemplaza "TuUnidadDePersistenciaPU" con el nombre de tu unidad de persistencia
        try {
            emf = Persistence.createEntityManagerFactory("com.mycompany_Fact_war_1.0-SNAPSHOTPU");
            clienteController = new ClienteJpaController(emf);
        } catch (Exception e) {
            throw new ServletException("Error inicializando EntityManagerFactory en LoginServlet", e);
        }
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        
        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");

        String logiClie = request.getParameter("logiClie");
        String passClie = request.getParameter("passClie");

        JsonObjectBuilder jsonBuilder = Json.createObjectBuilder();

        if (logiClie == null || logiClie.trim().isEmpty() || passClie == null || passClie.trim().isEmpty()) {
            jsonBuilder.add("success", false)
                       .add("message", "Usuario y contraseña son requeridos.");
        } else {
            try {
                Cliente cliente = clienteController.findClienteByLogin(logiClie);

                if (cliente != null && cliente.getPassClie().equals(passClie)) {
                    // Login exitoso
                    HttpSession session = request.getSession(true); // Crea una sesión si no existe
                    session.setAttribute("usuarioLogueado", cliente); // Guarda el objeto cliente
                    session.setAttribute("nombreUsuario", cliente.getNombClie()); // O un atributo más simple

                    // Preparamos información del usuario para enviar en el JSON (opcional)
                    JsonObjectBuilder userJsonBuilder = Json.createObjectBuilder();
                    userJsonBuilder.add("nombre", cliente.getNombClie())
                                   .add("apellidoPaterno", cliente.getAppaClie() != null ? cliente.getAppaClie() : "")
                                   .add("login", cliente.getLogiClie());

                    jsonBuilder.add("success", true)
                               .add("message", "Login exitoso. ¡Bienvenido!")
                               .add("user", userJsonBuilder);

                } else {
                    // Credenciales inválidas
                    jsonBuilder.add("success", false)
                               .add("message", "Usuario o contraseña incorrectos.");
                }
            } catch (Exception e) {
                // Error general del servidor o base de datos
                e.printStackTrace(); // Loguear el error real en el servidor
                jsonBuilder.add("success", false)
                           .add("message", "Error en el servidor al procesar la solicitud: " + e.getMessage());
            }
        }
        
        JsonObject jsonResponse = jsonBuilder.build();
        
        // Escribir el JSON en la respuesta
        try (StringWriter stringWriter = new StringWriter();
             JsonWriter jsonWriter = Json.createWriter(stringWriter)) {
            jsonWriter.writeObject(jsonResponse);
            try (PrintWriter out = response.getWriter()) {
                out.print(stringWriter.toString());
                out.flush();
            }
        }
    }

    @Override
    public void destroy() {
        if (emf != null && emf.isOpen()) {
            emf.close();
        }
        super.destroy();
    }

    @Override
    public String getServletInfo() {
        return "Servlet para manejar el login de clientes";
    }
}