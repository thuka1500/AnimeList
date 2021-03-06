package resources;

import entity.User;
import java.io.InputStream;
import java.net.URI;
import java.util.List;
import java.util.Set;
import javax.annotation.Resource;
import javax.enterprise.context.RequestScoped;
import javax.json.Json;
import javax.json.JsonException;
import javax.json.JsonObject;
import javax.json.JsonReader;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.persistence.TypedQuery;
import javax.transaction.Transactional;
import javax.validation.ConstraintViolation;
import javax.validation.Validator;
import javax.ws.rs.BadRequestException;
import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.DefaultValue;
import javax.ws.rs.ForbiddenException;
import javax.ws.rs.GET;
import javax.ws.rs.NotFoundException;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.Cookie;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.NewCookie;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.SecurityContext;
import validation.OnPasswordUpdate;
/*
 * Deze klasse stelt de JAX-RS resource "users" voor.
 */

@RequestScoped
@Path("users")
@Transactional(dontRollbackOn = {BadRequestException.class,
    ForbiddenException.class, NotFoundException.class})
public class Users
{
    @PersistenceContext
    private EntityManager em;
    
    @Resource
    private Validator validator;
    
    @Context
    private SecurityContext context;
    
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public List<User> getAllUsers(
            @QueryParam("first") @DefaultValue("0") int first,
            @QueryParam("results") @DefaultValue("10") int results)
    {
        TypedQuery<User> queryFindAll = 
                em.createNamedQuery("User.findAll", User.class);
        queryFindAll.setFirstResult(first);
        queryFindAll.setMaxResults(results);
        return queryFindAll.getResultList();
    }
    
    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    public Response addUser(User user)
    {
       Set<ConstraintViolation<User>> violations = 
                validator.validate(user, OnPasswordUpdate.class);
        if (!violations.isEmpty()) {
            throw new BadRequestException("Ongeldige invoer");
        }
        
        if (em.find(User.class, user.getUsername()) != null) {
            throw new BadRequestException("Username al in gebruik");
        }
        user.getRoles().add("user");
        
        em.persist(user);
        
        return Response.created(URI.create("/" + user.getUsername()))
                .build();
    }
    
    
    @Path("{username}")
    @DELETE
    public void removeUser(@PathParam("username") String username)
    {
        if (!context.getUserPrincipal().getName().equals(username)
                && !context.isUserInRole("admin")) {
            throw new ForbiddenException();
        }
        
        User user = em.find(User.class, username);
        
        if (user == null) {
            throw new NotFoundException("Gebruiker niet gevonden");
        }
        
        em.remove(user);
    }
}
