package br.com.barbeirofinanceiro.persistence;

import br.com.barbeirofinanceiro.domain.caixa.*;
import br.com.barbeirofinanceiro.domain.cliente.Cliente;
import br.com.barbeirofinanceiro.domain.cliente.ClienteRepository;
import br.com.barbeirofinanceiro.domain.item.*;
import br.com.barbeirofinanceiro.domain.usuario.Usuario;
import br.com.barbeirofinanceiro.domain.usuario.UsuarioRepository;
import br.com.barbeirofinanceiro.domain.venda.*;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
class VendaControllerIntegrationTest extends PostgresPersistenceTest {
    private static final String USER = "venda-user@teste.local";
    @Autowired MockMvc mockMvc;
    @Autowired ObjectMapper mapper;
    @Autowired UsuarioRepository usuarioRepository;
    @Autowired ClienteRepository clienteRepository;
    @Autowired ItemRepository itemRepository;
    @Autowired CaixaRepository caixaRepository;
    @Autowired VendaRepository vendaRepository;
    @Autowired ItemVendaRepository itemVendaRepository;
    @Autowired VendaPagamentoRepository pagamentoRepository;

    @BeforeEach void preparar() {
        Usuario u = newEntity(Usuario.class); u.setNome("Vendedor"); u.setEmail(USER); u.setSenhaHash("hash"); usuarioRepository.saveAndFlush(u);
        Caixa c = new Caixa(); c.setDataCaixa(LocalDate.now()); c.setValorInicial(BigDecimal.valueOf(100)); c.setStatus(StatusCaixa.ABERTO); c.setUsuarioAbertura(u); caixaRepository.saveAndFlush(c);
    }
    @AfterEach void limpar() { pagamentoRepository.deleteAll(); itemVendaRepository.deleteAll(); vendaRepository.deleteAll(); caixaRepository.deleteAll(); itemRepository.deleteAll(); clienteRepository.deleteAll(); usuarioRepository.deleteAll(); }

    @Test void deveRegistrarVendaComServicoEPrecoHistorico() throws Exception {
        UUID item = item("Corte", TipoItem.SERVICO, "30", null);
        UUID id = criarVenda(item, 1, "PIX", "30");
        mockMvc.perform(get("/api/v1/vendas/{id}", id).with(user(USER))).andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("FINALIZADA"))
                .andExpect(jsonPath("$.valorTotal").value(30.0))
                .andExpect(jsonPath("$.itens[0].precoUnitario").value(30.0));
        assertThat(itemRepository.findById(item).get().getEstoque()).isNull();
    }

    @Test void deveBaixarEstoqueERegistrarVendaMista() throws Exception {
        UUID servico = item("Corte", TipoItem.SERVICO, "30", null);
        UUID produto = item("Pomada", TipoItem.PRODUTO, "35", 10);
        UUID cliente = cliente("João");
        String body = "{\"clienteId\":\"" + cliente + "\",\"itens\":[{\"itemId\":\"" + servico + "\",\"quantidade\":1},{\"itemId\":\"" + produto + "\",\"quantidade\":2}],\"pagamentos\":[{\"formaPagamento\":\"DINHEIRO\",\"valor\":100}]}";
        mockMvc.perform(post("/api/v1/vendas").with(csrf()).with(user(USER)).contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isCreated()).andExpect(jsonPath("$.valorTotal").value(100.0));
        assertThat(itemRepository.findById(produto).get().getEstoque()).isEqualTo(8);
    }

    @Test void deveAceitarPagamentoDividido() throws Exception {
        UUID item = item("Corte", TipoItem.SERVICO, "30", null);
        mockMvc.perform(post("/api/v1/vendas").with(csrf()).with(user(USER)).contentType(MediaType.APPLICATION_JSON)
                .content("{\"itens\":[{\"itemId\":\"" + item + "\",\"quantidade\":1}],\"pagamentos\":[{\"formaPagamento\":\"PIX\",\"valor\":10},{\"formaPagamento\":\"DINHEIRO\",\"valor\":20}]}"))
                .andExpect(status().isCreated()).andExpect(jsonPath("$.pagamentos", org.hamcrest.Matchers.hasSize(2)));
    }

    @Test void deveRejeitarPagamentoDivergenteEQuantidadeInvalida() throws Exception {
        UUID item = item("Corte", TipoItem.SERVICO, "30", null);
        String base = "\"itens\":[{\"itemId\":\"" + item + "\",\"quantidade\":%s}],\"pagamentos\":[{\"formaPagamento\":\"PIX\",\"valor\":29}]";
        criarEsperando("{" + base.formatted("1") + "}", 400);
        criarEsperando("{" + base.formatted("0") + "}", 400);
    }

    @Test void deveRejeitarItemInativoEEstoqueInsuficiente() throws Exception {
        UUID item = item("Pomada", TipoItem.PRODUTO, "35", 1); Item produto = itemRepository.findById(item).get(); produto.setAtivo(false); itemRepository.saveAndFlush(produto);
        criarEsperando("{\"itens\":[{\"itemId\":\"" + item + "\",\"quantidade\":1}],\"pagamentos\":[{\"formaPagamento\":\"PIX\",\"valor\":35}]}", 409);
        produto.setAtivo(true); itemRepository.saveAndFlush(produto);
        criarEsperando("{\"itens\":[{\"itemId\":\"" + item + "\",\"quantidade\":2}],\"pagamentos\":[{\"formaPagamento\":\"PIX\",\"valor\":70}]}", 409);
    }

    @Test void deveCancelarERestaurarEstoqueSemPermitirSegundoCancelamento() throws Exception {
        UUID item = item("Pomada", TipoItem.PRODUTO, "35", 2); UUID venda = criarVenda(item, 1, "PIX", "35");
        mockMvc.perform(post("/api/v1/vendas/{id}/cancelar", venda).with(csrf()).with(user(USER)))
                .andExpect(status().isOk()).andExpect(jsonPath("$.status").value("CANCELADA"));
        assertThat(itemRepository.findById(item).get().getEstoque()).isEqualTo(2);
        mockMvc.perform(post("/api/v1/vendas/{id}/cancelar", venda).with(csrf()).with(user(USER)))
                .andExpect(status().isConflict());
    }

    private UUID criarVenda(UUID item, int quantidade, String forma, String valor) throws Exception {
        String json = "{\"itens\":[{\"itemId\":\"" + item + "\",\"quantidade\":" + quantidade + "}],\"pagamentos\":[{\"formaPagamento\":\"" + forma + "\",\"valor\":" + valor + "}]}";
        MvcResult r = mockMvc.perform(post("/api/v1/vendas").with(csrf()).with(user(USER)).contentType(MediaType.APPLICATION_JSON).content(json)).andExpect(status().isCreated()).andReturn();
        return UUID.fromString(mapper.readTree(r.getResponse().getContentAsString()).get("id").asText());
    }
    private void criarEsperando(String json, int code) throws Exception { mockMvc.perform(post("/api/v1/vendas").with(csrf()).with(user(USER)).contentType(MediaType.APPLICATION_JSON).content(json)).andExpect(status().is(code)); }
    private UUID item(String nome, TipoItem tipo, String preco, Integer estoque) { Item i=new Item(); i.setNome(nome); i.setTipo(tipo); i.setPreco(new BigDecimal(preco)); i.setEstoque(estoque); return itemRepository.saveAndFlush(i).getId(); }
    private UUID cliente(String nome) { Cliente c=newEntity(Cliente.class); c.setNome(nome); return clienteRepository.saveAndFlush(c).getId(); }
}
