/*
 * LICENSE INFORMATION
 * Copyright 2005-2007 by FZI (http://www.fzi.de).
 * Licensed under a BSD license (http://www.opensource.org/licenses/bsd-license.php)
 * <OWNER> = Max Völkel
 * <ORGANIZATION> = FZI Forschungszentrum Informatik Karlsruhe, Karlsruhe, Germany
 * <YEAR> = 2007
 * 
 * Project information at http://semweb4j.org/rdf2go
 */
package org.ontoware.rdf2go.model;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.StringWriter;
import java.util.ArrayList;

import junit.framework.Assert;
import junit.framework.TestCase;

import org.junit.Test;
import org.ontoware.aifbcommons.collection.ClosableIterable;
import org.ontoware.aifbcommons.collection.ClosableIterator;
import org.ontoware.rdf2go.ModelFactory;
import org.ontoware.rdf2go.exception.ModelRuntimeException;
import org.ontoware.rdf2go.model.node.URI;
import org.ontoware.rdf2go.model.node.Variable;
import org.ontoware.rdf2go.model.node.impl.URIImpl;
import org.ontoware.rdf2go.testdata.TestData;
import org.ontoware.rdf2go.util.Iterators;
import org.ontoware.rdf2go.util.ModelUtils;
import org.ontoware.rdf2go.vocabulary.RDFS;

/**
 * Use this to test implementations of ModelSet. Overwrite the class and
 * implement the "setup" method, initializing modelset and modelfactory.
 * 
 * @author voelkel
 * @author sauermann
 */
public abstract class AbstractModelSetTest extends TestCase {

	/**
	 * 
	 * @return a fresh ModelSet, to be tested
	 */
	public abstract ModelFactory getModelFactory();

	/**
	 * 
	 */
	public AbstractModelSetTest() {
		super();
	}

	/**
	 * @param arg0
	 */
	public AbstractModelSetTest(String arg0) {
		super(arg0);
	}

	public static URI graphuri1 = new URIImpl("urn:first");

	public static URI graphuri2 = new URIImpl("urn:second");

	/**
	 * there are two graphs in the default test data
	 */
	public static int TESTGRAPHCOUNT = 2;

	public static URI a = new URIImpl("test://test/a");

	public static URI b = new URIImpl("test://test/b");

	public static URI c = new URIImpl("test://test/c");

	public static URI subject = new URIImpl("test://test/a");

	public static URI predicate = new URIImpl("test://test/b");

	public static URI object = new URIImpl("test://test/c");

	public static URI dt = new URIImpl("test://somedata/dt");

	protected ModelSet modelset = null;

	@Override
	public void setUp() {
		// done by each test, to allow for different Reasoning settings
	}

	@Override
	public void tearDown() throws Exception {
		if (this.modelset != null) {
			this.modelset.close();
			this.modelset = null;
		}
		System.gc();
	}

	/**
	 * Add some test data to the modelsets . Test data will use graphuri1,
	 * graphuri2
	 * 
	 */
	protected void addTestDataToModelSet() throws Exception {
		assertNotNull("should be initialised by test method already", this.modelset);
		// add two models
		Model foaf = TestData.loadFoafBuffered(getModelFactory());
		Model m = this.modelset.getModel(graphuri1);
		m.open();
		m.addAll(foaf.iterator());
		Model ical = TestData.loadICALBuffered(getModelFactory());
		m = this.modelset.getModel(graphuri2);
		m.open();
		m.addAll(ical.iterator());
		assertTrue("the test data works", foaf.size() > 10);
		assertTrue("the test data works", ical.size() > 10);
		assertTrue("the modelset contains some triples", this.modelset.size() > 20);
		assertEquals(foaf.size() + ical.size(), this.modelset.size());
		// never close them when you loaded them buffered
		// foaf.close();
		// ical.close();
		m.close();
	}

	/**
	 * The default model as defined in the SPARQL semantics. Each data set
	 * consisting of Models/Graphs has one default graph.
	 * 
	 */
	@Test
	public void testGetDefaultModel() {
		this.modelset = getModelFactory().createModelSet();
		this.modelset.open();
		Model defaultModel = this.modelset.getDefaultModel();
		assertNotNull(defaultModel);
		assertNull("the default model must have the NULL context", defaultModel
				.getContextURI());
		defaultModel.close();
	}

	@Test
	public void testGetModel() throws Exception {
		this.modelset = getModelFactory().createModelSet();
		this.modelset.open();
		addTestDataToModelSet();
		this.modelset.close();
		Model m = this.modelset.getModel(graphuri1);
		m.open();
		assertNotNull(m);
		assertTrue("graph1 contains less than 10 statements, it contains: "
				+ m.size(), m.size() > 10);
		m = this.modelset.getModel(graphuri2);
		m.open();
		assertNotNull(m);
		assertTrue("graph2 contains more than 10 statements, it contains: "
				+ m.size(), m.size() > 10);
		m.close();
	}

	@Test
	public void testRemoveModel() throws ModelRuntimeException {
		this.modelset = getModelFactory().createModelSet();
		this.modelset.open();
		Model m = this.modelset.getModel(graphuri1);
		m.open();
		assertEquals(0, m.size());
		m.addStatement(a, b, c);
		assertTrue(m.size() > 0);
		this.modelset.removeModel(graphuri1);
		m.close();
		m = this.modelset.getModel(graphuri1);
		m.open();
		assertEquals(0, m.size());
		m.close();
	}

	@Test
	public void testRemoveAll() throws ModelRuntimeException {
		this.modelset = getModelFactory().createModelSet();
		this.modelset.open();
		Model m = this.modelset.getModel(graphuri1);
		m.open();
		assertEquals(0, m.size());
		m.addStatement(a, b, c);
		assertTrue(m.size() > 0);
		this.modelset.removeAll();
		m.close();
		m = this.modelset.getModel(graphuri1);
		m.open();
		assertEquals(0, m.size());
		m.close();
	}

	@Test
	public void testGetModels() throws Exception {
		this.modelset = getModelFactory().createModelSet();
		this.modelset.open();
		addTestDataToModelSet();
		ClosableIterator<? extends Model> i = modelset.getModels();
		ArrayList<Model> m = new ArrayList<Model>();
		Iterators.addAll(i, m);
		assertEquals(TESTGRAPHCOUNT, m.size());
		i.close();
	}

	/** test find with (c,x,y,z) on contained model */
	@Test
	public void testFindStatements() throws ModelRuntimeException {
		this.modelset = getModelFactory().createModelSet();
		this.modelset.open();
		Model m = this.modelset.getModel(graphuri1);
		m.open();
		m.addStatement(a, b, c);
		ClosableIterator<? extends Statement> it = this.modelset.findStatements(
				graphuri1, a, b, c);
		assertTrue(it.hasNext());
		it.next();
		assertFalse(it.hasNext());
		it.close();
		m.close();
	}

	/**
	 * Test find with (*,x,y,z) on modelset
	 * 
	 * @throws ModelRuntimeException
	 */
	@Test
	public void testFindStatements2() throws ModelRuntimeException {
		this.modelset = getModelFactory().createModelSet();
		this.modelset.open();
		URI a = new URIImpl("urn:test:a");
		URI b = new URIImpl("urn:test:b");
		URI c = new URIImpl("urn:test:c");
		URI d = new URIImpl("urn:test:d");
		this.modelset.addStatement(a, b, c, d);

		ClosableIterator<? extends Statement> it = this.modelset.findStatements(
				Variable.ANY, b, c, d);
		Statement stmt = it.next();
		Assert.assertNotNull(stmt.getContext());
		Assert.assertEquals(a, stmt.getContext());
		it.close();
	}

	@Test
	public void testContainsStatements() throws ModelRuntimeException {
		this.modelset = getModelFactory().createModelSet();
		this.modelset.open();
		assertFalse(this.modelset.containsStatements(graphuri1, a, b, c));
		Model model = this.modelset.getModel(graphuri1);
		model.open();
		model.addStatement(a, b, c);
		assertTrue(this.modelset.containsStatements(graphuri1, a, b, c));

		// also with wildcard
		assertTrue(this.modelset.containsStatements(Variable.ANY, a, b, c));

		model.removeStatement(a, b, c);
		//		
		assertFalse(this.modelset.containsStatements(graphuri1, a, b, c));
		model.close();
	}

	@Test
	public void testSparqlAsk() {
		this.modelset = getModelFactory().createModelSet();
		this.modelset.open();
		// TODO add test when sparql ask is availalbe (not yet, as of 17.08.2007)
		
		//fail("Not yet implemented");
	}

	@Test
	public void testSparqlConstruct() throws Exception {
		this.modelset = getModelFactory().createModelSet();
		this.modelset.open();

		addTestDataToModelSet();
		ClosableIterable<? extends Statement> i = this.modelset
				.sparqlConstruct("PREFIX rdfs:    <http://www.w3.org/2000/01/rdf-schema#>\n "
						+ "PREFIX rdf:     <http://www.w3.org/1999/02/22-rdf-syntax-ns#> \n"
						+ "construct {?s rdf:type ?o} where {?s rdf:type ?o}");
		int size = ModelUtils.size(i);
		assertEquals("sparql construct works getting types", 395, size);
	}

	@Test
	public void testSparqlDescribe() {
		this.modelset = getModelFactory().createModelSet();
		this.modelset.open();
		// TODO add test when sparql describe is available (not yet, as of 17.08.2007)
		
		//fail("Not yet implemented");
		
	}

	@Test
	public void testSparqlSelect() throws ModelRuntimeException {
		this.modelset = getModelFactory().createModelSet();
		this.modelset.open();
		Model m = this.modelset.getDefaultModel();
		m.open();
		m.addStatement(a, b, c);

		QueryResultTable table = this.modelset
				.sparqlSelect("SELECT ?s ?p ?o WHERE { ?s ?p ?o . }");
		ClosableIterator<QueryRow> it = table.iterator();
		assertTrue(it.hasNext());
		it.next();
		assertFalse(it.hasNext());
		it.close();
	}

	@Test
	public void testSparqlSelectFOAF() throws ModelRuntimeException,
			IOException {
		this.modelset = getModelFactory().createModelSet();
		this.modelset.open();

		// URL foafURL = new URL("http://xmlns.com/foaf/0.1/20050603.rdf");
		// this.modelset.readFrom( foafURL.openStream() );
		InputStream in = TestData.getFoafAsStream();
		this.modelset.readFrom(in);

		QueryResultTable table = this.modelset
				.sparqlSelect("SELECT ?s ?o WHERE { ?s <" + RDFS.comment
						+ "> ?o . }");
		ClosableIterator<QueryRow> it = table.iterator();
		assertTrue(it.hasNext());
		it.next();
		assertTrue(it.hasNext());
		it.close();
	}

	@Test
	public void testGetUnderlyingModelSetImplementation() {
		this.modelset = getModelFactory().createModelSet();
		this.modelset.open();
		assertNotNull(this.modelset.getUnderlyingModelSetImplementation());
	}

	@Test
	public void testCreateURI() throws ModelRuntimeException {
		this.modelset = getModelFactory().createModelSet();
		this.modelset.open();
		URI u = this.modelset.createURI("urn:test:x");
		assertNotNull(u);
	}

// TODO (wth, 15.08.2007) should all this tests which state: "write test here" be written? yes
	
	@Test
	public void testOpenClose() {
		this.modelset = getModelFactory().createModelSet();
		this.modelset.open();

		// TODO write test here
		// tricky... a lot of things have to be considered - ask max
	}

	@Test
	public void testGetModelURIs() throws Exception {
		this.modelset = getModelFactory().createModelSet();
		this.modelset.open();
		addTestDataToModelSet();
		ClosableIterator<URI> l = this.modelset.getModelURIs();
		ArrayList<URI> test = new ArrayList<URI>();
		test.add(graphuri1);
		test.add(graphuri2);
		ArrayList<URI> uris = new ArrayList<URI>();
		Iterators.addAll(l, uris);
		assertEquals(2, uris.size());
		test.removeAll(uris);
		assertEquals(0, test.size());
		l.close();		
	}

	@Test
	public void testReadFromReader() {
		this.modelset = getModelFactory().createModelSet();
		this.modelset.open();

		InputStreamReader isr = new InputStreamReader(TestData
				.getFoafAsStream());
		try {
			this.modelset.readFrom(isr);
		} catch (ModelRuntimeException e) {
			fail();
		} catch (IOException e) {
			fail();
		} finally {
			try {
				isr.close();
			} catch (IOException e) {
				fail();
			}
		}
	}

	@Test
	public void testReadFromReaderSyntax() {
		this.modelset = getModelFactory().createModelSet();
		this.modelset.open();

		// TODO write test here
	}

	@Test
	public void testReadFromInputStream() {
		this.modelset = getModelFactory().createModelSet();
		this.modelset.open();
		InputStream in = TestData.getFoafAsStream();
		try {
			this.modelset.readFrom(in);
		} catch (ModelRuntimeException e) {
			fail();
		} catch (IOException e) {
			fail();
		} finally {
			try {
				in.close();
			} catch (IOException e) {
				fail();
			}
		}
	}

	@Test
	public void testReadFromInputStreamIntoDefaultModel() throws Exception {
		this.modelset = getModelFactory().createModelSet();
		this.modelset.open();
		this.modelset.readFrom(TestData.getFoafAsStream(), Syntax.RdfXml);

		assertTrue(this.modelset.size() > 0);
		// the modelset loads into the default model
		assertFalse(
				"as we loaded data only in default model, there are no other models",
				this.modelset.getModels().hasNext());

		// the default model has something
		Model m = this.modelset.getDefaultModel();
		m.open();
		assertEquals("the default model has foaf", 536, m.size());

		ClosableIterator<Statement> i = m.iterator();
		int sizeByIterator = ModelUtils.size(i);
		assertEquals("the default model can use an iterator", 536,
				sizeByIterator);
		i.close();
	}

	@Test
	public void testReadFromInputStreamIntoNamedModel() throws Exception {
		this.modelset = getModelFactory().createModelSet();
		this.modelset.open();
		Model m = this.modelset.getModel(graphuri1);
		m.open();
		m.readFrom(TestData.getFoafAsStream(), Syntax.RdfXml);
		m.close();

		// add data to second graph
		m = this.modelset.getModel(graphuri2);
		m.open();
		m.readFrom(TestData.getICALAsStream(), Syntax.RdfXml);
		m.close();

		// assertEquals("The modelset contains exactly loaded triples.",
		// TestData.FOAF_SIZE + TestData.ICALSIZE, this.modelset.size());
		// the modelset loads into the named graph
		int modelcount = ModelUtils.size(this.modelset.getModels());
		assertEquals("there are two models", 2, modelcount);
		// no default model
		m = this.modelset.getDefaultModel();
		m.open();
		assertEquals("nothing in default model", 0, m.size());
		m.close();

		// get the named model1
		m = this.modelset.getModel(graphuri1);
		m.open();
		assertEquals("the named graph model has foaf", TestData.FOAFSIZE, m
				.size());
		// at some point, iterators were broken here, so test if it returns one
		ClosableIterator<Statement> i = m.iterator();
		int sizeByIterator = ModelUtils.size(i);
		assertEquals("the model supports iterators", TestData.FOAFSIZE,
				sizeByIterator);
		i.close();
		m.close();

		// get the named model2
		m = this.modelset.getModel(graphuri2);
		m.open();
		assertEquals("the named graph model has ical", TestData.ICALSIZE, m
				.size());
		m.close();

	}

	@Test
	public void testReadFromInputStreamSyntax() {
		this.modelset = getModelFactory().createModelSet();
		this.modelset.open();

		// TODO write test here
	}

	@Test
	public void testSize() throws Exception {
		this.modelset = getModelFactory().createModelSet();
		this.modelset.open();
		this.modelset.readFrom(TestData.getFoafAsStream());
		assertEquals("Size of foaf", 536, this.modelset.size());
	}

	@Test
	public void testWriteToWriter() throws Exception {
		this.modelset = getModelFactory().createModelSet();
		this.modelset.open();
		addTestDataToModelSet();

		StringWriter sw = new StringWriter();
		try {
			this.modelset.writeTo(sw);
		} catch (ModelRuntimeException e) {
			fail();
		} catch (IOException e) {
			fail();
		}
		assertTrue(sw.getBuffer().toString().length() > 1000);
		sw.close();
	}

	@Test
	public void testWriteToWriterSyntax() {
		this.modelset = getModelFactory().createModelSet();
		this.modelset.open();

		// TODO write test
	}

	@Test
	public void testWriteToOutputStream() throws Exception {
		this.modelset = getModelFactory().createModelSet();
		this.modelset.open();
		addTestDataToModelSet();

		try {
			this.modelset.writeTo(System.out);
		} catch (ModelRuntimeException e) {
			fail();
		} catch (IOException e) {
			fail();
		}
	}

	@Test
	public void testWriteToOutputStreamSyntax() throws Exception {
		this.modelset = getModelFactory().createModelSet();
		this.modelset.open();
		addTestDataToModelSet();

		try {
			this.modelset.writeTo(System.out, Syntax.Turtle);
		} catch (ModelRuntimeException e) {
			fail();
		} catch (IOException e) {
			fail();
		}
	}

	@Test
	public void testLoadDataIntoDefaultModel() throws Exception {
		this.modelset = getModelFactory().createModelSet();
		this.modelset.open();
		this.modelset.readFrom(TestData.getFoafAsStream());
		assertTrue(this.modelset.size() > 10);
		// check default model
		Model def = this.modelset.getDefaultModel();
		def.open();
		assertTrue(def.isOpen());
		assertNotNull(def);
		assertTrue("default model has something", def.size() > 10);
		ArrayList<URI> uris = new ArrayList<URI>();
		Iterators.addAll(this.modelset.getModelURIs(), uris);
		assertEquals("default model has no context uri", 0, uris.size());
		def.close();
	}

	@Test
	public void testCopyModelSets() throws Exception {
		this.modelset = getModelFactory().createModelSet();
		this.modelset.open();
		addTestDataToModelSet();
		// add a statement in the default context
		Model model = this.modelset.getDefaultModel();
		model.open();
		model.addStatement(subject, predicate, object);
		ModelSet m = getModelFactory().createModelSet();
		m.open();
		ModelUtils.copy(this.modelset, m);
		assertEquals("copied all from source to target", this.modelset.size(), m
				.size());
		m.close();
	}

	@Test
	public void testAddDataFromFileByCopying() throws Exception {
		this.modelset = getModelFactory().createModelSet();
		this.modelset.open();
		this.modelset.readFrom(TestData.getFoafAsStream(), Syntax.RdfXml);
		assertTrue(this.modelset.size() > 0);

		ModelSet target = getModelFactory().createModelSet();
		target.open();

		ModelUtils.copy(this.modelset, target);
		assertEquals(this.modelset.size(), target.size());
		target.close();
	}

	/**
	 * above test sometimes failed, here is a trace into the details that can
	 * happen
	 * 
	 * @throws Exception
	 */
	@Test
	public void testAddDataFromFileByCopyingMoreDetail() throws Exception {
		this.modelset = getModelFactory().createModelSet();
		this.modelset.open();
		this.modelset.readFrom(TestData.getFoafAsStream(), Syntax.RdfXml);

		assertTrue(this.modelset.size() > 0);
		// the modelset loads into the default model
		assertFalse(
				"as we loaded data only in default model, there are no other models",
				this.modelset.getModels().hasNext());

		// the default model has something
		Model m = this.modelset.getDefaultModel();
		m.open();
		assertEquals("the default model has foaf", 536, m.size());
		int sizeByIterator = ModelUtils.size(m);
		assertEquals("the default model can use an iterator", 536,
				sizeByIterator);
		m.close();

		ModelSet target = getModelFactory().createModelSet();
		target.open();

		// Es liegt also daran, das modelset.read die daten in irgendein nicht
		// mehr
		// zugreifbared modell mit dem context "null" steckt. Aber wenn man ein
		// model
		// mti "null" macht, ist es leer. Da stimmt was nicht mit sesame.
		ModelUtils.copy(this.modelset, target);

		assertEquals(this.modelset.size(), target.size());
		m.close();
	}
}
