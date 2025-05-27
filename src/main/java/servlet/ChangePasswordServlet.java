package servlet;

import dao.ClienteJpaController;
import dao.exceptions.NonexistentEntityException;
import dto.Cliente;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import javax.json.Json;
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

@WebServlet(name = "ChangePasswordServlet", urlPatterns = {"/ChangePasswordServlet"})
public class ChangePasswordServlet extends HttpServlet {

    private EntityManagerFactory emf;
    private ClienteJpaController clienteController;

    @Override
    public void init() throws ServletException {
        super.init();
        try {
            // Reemplaza "TuUnidadDePersistenciaPU" con el nombre de tu unidad de persistencia
            emf = Persistence.createEntityManagerFactory("com.mycompany_Fact_war_1.0-SNAPSHOTPU");
            clienteController = new ClienteJpaController(emf);
        } catch (Exception e) {
            throw new ServletException("Error inicializando EntityManagerFactory en ChangePasswordServlet", e);
        }
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");
        JsonObjectBuilder jsonBuilder = Json.createObjectBuilder();

        HttpSession session = request.getSession(false); // No crear nueva sesión si no existe

        if (session == null || session.getAttribute("usuarioLogueado") == null) {
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED); // 401 No autorizado
            jsonBuilder.add("success", false)
                       .add("message", "No estás autenticado. Por favor, inicia sesión primero.");
            writeJsonResponse(response, jsonBuilder.build());
            return;
        }

        Cliente usuarioLogueado = (Cliente) session.getAttribute("usuarioLogueado");

        String currentPassword = request.getParameter("currentPassword");
        String newPassword = request.getParameter("newPassword");
        String confirmNewPassword = request.getParameter("confirmNewPassword");

        // Validaciones del lado del servidor
        if (currentPassword == null || currentPassword.trim().isEmpty() ||
            newPassword == null || newPassword.trim().isEmpty() ||
            confirmNewPassword == null || confirmNewPassword.trim().isEmpty()) {
            jsonBuilder.add("success", false)
                       .add("message", "Todos los campos son obligatorios.");
        } else if (newPassword.length() < 6) { // Replicar política del cliente
             jsonBuilder.add("success", false)
                       .add("message", "La nueva contraseña debe tener al menos 6 caracteres.");
        } else if (!newPassword.equals(confirmNewPassword)) {
            jsonBuilder.add("success", false)
                       .add("message", "Las nuevas contraseñas no coinciden.");
        } else if (!usuarioLogueado.getPassClie().equals(currentPassword)) {
            // ¡IMPORTANTE! Esto compara contraseñas en texto plano.
            // En producción, deberías comparar hashes de contraseñas.
            jsonBuilder.add("success", false)
                       .add("message", "La contraseña actual es incorrecta.");
        } else if (usuarioLogueado.getPassClie().equals(newPassword)) {
            jsonBuilder.add("success", false)
                       .add("message", "La nueva contraseña no puede ser igual a la contraseña actual.");
        }
        else {
            try {
                // **ADVERTENCIA DE SEGURIDAD:**
                // Estás guardando la contraseña en texto plano. ¡Esto es muy inseguro!
                // Deberías hashear la nueva contraseña antes de guardarla.
                // Ejemplo (necesitarías una librería como BCrypt):
                // String hashedPassword = BCrypt.hashpw(newPassword, BCrypt.gensalt());
                // usuarioLogueado.setPassClie(hashedPassword);

                usuarioLogueado.setPassClie(newPassword); // Guardando en texto plano (NO RECOMENDADO)
                
                clienteController.edit(usuarioLogueado); // Guardar los cambios en la BD

                jsonBuilder.add("success", true)
                           .add("message", "Contraseña cambiada exitosamente.");

            } catch (NonexistentEntityException ne) {
                 jsonBuilder.add("success", false)
                           .add("message", "Error: El usuario no existe en la base de datos.");
            } 
            catch (Exception e) {
                e.printStackTrace(); // Loguear el error real en el servidor
                jsonBuilder.add("success", false)
                           .add("message", "Error en el servidor al cambiar la contraseña: " + e.getMessage());
            }
        }
        
        writeJsonResponse(response, jsonBuilder.build());
    }

    private void writeJsonResponse(HttpServletResponse response, JsonObject jsonObject) throws IOException {
        try (StringWriter stringWriter = new StringWriter();
             JsonWriter jsonWriter = Json.createWriter(stringWriter)) {
            jsonWriter.writeObject(jsonObject);
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
        return "Servlet para cambiar la contraseña del cliente autenticado";
    }
}