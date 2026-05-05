package jp.igapyon.mikutextbundle.match;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Arrays;
import java.util.List;

import org.junit.jupiter.api.Test;

class PatternMatcherTest {
    @Test
    void matchesSimpleGlobPatterns() {
        assertTrue(PatternMatcher.matchesAnyPattern("docs/readme.md", Arrays.asList("docs/**/*.md")));
        assertFalse(PatternMatcher.matchesAnyPattern("src/main.ts", Arrays.asList("test/**")));
    }

    @Test
    void matchesRootGitignoreDirectoryAndFilePatterns() {
        List<String> patterns = PatternMatcher.parseGitignore("node_modules/\nignored.ts\n# comment\n");

        assertTrue(PatternMatcher.matchesGitignore("node_modules/pkg/index.js", patterns));
        assertTrue(PatternMatcher.matchesGitignore("src/ignored.ts", patterns));
        assertFalse(PatternMatcher.matchesGitignore("src/main.ts", patterns));
    }

    @Test
    void matchesBasenameGlobsAtAnyDepth() {
        List<String> patterns = PatternMatcher.parseGitignore("*.log\n*.tmp\n");

        assertTrue(PatternMatcher.matchesGitignore("debug.log", patterns));
        assertTrue(PatternMatcher.matchesGitignore("logs/debug.log", patterns));
        assertFalse(PatternMatcher.matchesGitignore("src/main.ts", patterns));
    }

    @Test
    void matchesRootedPatternsOnlyFromRepositoryRoot() {
        List<String> patterns = PatternMatcher.parseGitignore("/dist/\n/root-only.ts\n");

        assertTrue(PatternMatcher.matchesGitignore("dist/main.js", patterns));
        assertFalse(PatternMatcher.matchesGitignore("pkg/dist/main.js", patterns));
        assertTrue(PatternMatcher.matchesGitignore("root-only.ts", patterns));
        assertFalse(PatternMatcher.matchesGitignore("src/root-only.ts", patterns));
    }

    @Test
    void matchesNestedPathGlobs() {
        List<String> patterns = PatternMatcher.parseGitignore("generated/**/*.ts\n");

        assertTrue(PatternMatcher.matchesGitignore("generated/main.ts", patterns));
        assertTrue(PatternMatcher.matchesGitignore("generated/deep/main.ts", patterns));
        assertTrue(PatternMatcher.matchesGitignore("src/generated/main.ts", patterns));
    }
}
