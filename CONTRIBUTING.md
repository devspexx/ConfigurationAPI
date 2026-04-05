## Contributing to ConfigurationAPI

Thank you for your interest in contributing to ConfigurationAPI!  
All contributions are welcome — bug fixes, improvements, and new ideas.

---

#### Guidelines

To keep the project clean and maintainable, please follow these principles:

- Write **clear, readable, and consistent code**
- Prefer **simple and predictable implementations**
- Maintain **thread-safety guarantees**
- Avoid unnecessary complexity or over-engineering

#### Development Setup
Requirements:
- Java 21+
- Maven 3+

Build the project:

```bash
mvn clean package
```

#### Workflow
1. Fork the repository (use a personal fork, not an organization)
2. Create a new branch:
3. `git checkout -b feature/my-change`
4. Make your changes
5. Commit using clear messages: `git commit -m "feat: improve config loading performance"`
6. Push and open a Pull Request


#### Code Style
- Follow standard Java conventions (IDE defaults are fine)
- Keep methods focused and small
- Use meaningful names
- Avoid var where clarity suffers
- Add Javadoc for all public APIs

#### Documentation
- All public classes and methods must include Javadoc
- Javadoc must be doclint-compliant (no invalid HTML)
- Include `@since` where appropriate


#### Testing
- Ensure your changes do not break existing behavior
- If applicable, test:
  - config reloads
  - invalid YAML handling
  - watcher behavior

#### Pull Request Notes
> Keep PRs focused and minimal
- Explain why the change is needed
- Large changes should be discussed in an issue first


#### What Not to Do
- Do not introduce breaking changes without discussion
- Do not add unnecessary dependencies
- Do not change public API behavior without justification

#### Questions
If you're unsure about something, open an issue first — happy to help.

---

### Thanks for contributing ❤️
