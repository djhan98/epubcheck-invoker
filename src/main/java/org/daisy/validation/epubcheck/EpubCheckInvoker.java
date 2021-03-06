package org.daisy.validation.epubcheck;

import java.io.File;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

import org.daisy.validation.epubcheck.Issue.Type;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.Lists;
import com.google.common.util.concurrent.UncheckedTimeoutException;

public final class EpubCheckInvoker {

	private static final Logger LOG = LoggerFactory
			.getLogger(EpubCheckInvoker.class);

	private static final EpubCheckInvoker INSTANCE = new EpubCheckInvoker();

	public static List<Issue> run(String file) {
		return INSTANCE.validate(file);
	}

	private Configuration config = Configuration.newConfiguration();

	public List<Issue> validate(String epubPath) {
		return validate(new File(epubPath));
	}

	public List<Issue> validate(final File epubFile) {
		Future<List<Issue>> result = config.executorService.get().submit(
				new Callable<List<Issue>>() {

					@Override
					public List<Issue> call() {
						return doValidate(epubFile);
					}
				});
		try {
			return result.get();
		} catch (InterruptedException e) {
			return Lists.newArrayList(new Issue(Type.INTERNAL_ERROR,
					"InterruptedException - " + e.getMessage()));
		} catch (ExecutionException e) {
			// Shouldn't happen
			throw new RuntimeException(e);
		}
	}

	private List<Issue> doValidate(File epub) {
		LOG.info("Validating {}", epub);
		CommandExecutor<List<Issue>> cmdExec = new CommandExecutor<List<Issue>>(
				Lists.newArrayList("java", "-jar", config.jar.get(),
						epub.getPath()));
		try {
			return cmdExec.run(new StatefulParser(epub),config.timeout.get(),config.timeoutUnit.get());
		} catch (InterruptedException e) {
			return Lists.newArrayList(new Issue(Type.INTERNAL_ERROR,
					"InterruptedException - " + e.getMessage()));
		} catch (UncheckedTimeoutException e) {
			return Lists.newArrayList(new Issue(Type.INTERNAL_ERROR,
					"Process timed out"));
		} catch (Exception e) {
			return Lists.newArrayList(new Issue(Type.INTERNAL_ERROR,
					
					e.getCause().getClass().getSimpleName() + " - " + e.getMessage()));
		}
	}
}
