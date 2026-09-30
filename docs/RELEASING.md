# Publishing and releasing

## One-time: put the repository on GitHub

1. **SSH key.** Skip this if you already push to GitHub over SSH (`ssh -T git@github.com` greets you by name).

   ```sh
   ssh-keygen -t ed25519 -C "kenthejr@gmail.com"      # accept the default path; set a passphrase
   eval "$(ssh-agent -s)" && ssh-add ~/.ssh/id_ed25519
   cat ~/.ssh/id_ed25519.pub                          # copy this
   ```

   Add the public key at <https://github.com/settings/keys> as an **Authentication key**. Optionally add it again as a
   **Signing key** if you want GitHub to show your commits as *Verified*:

   ```sh
   git config --global gpg.format ssh
   git config --global user.signingkey ~/.ssh/id_ed25519.pub
   git config --global commit.gpgsign true
   ```

2. **Create the repository** at <https://github.com/new> named `infinity-castle`. Leave it empty, with no README or
   license, since both already exist here.

3. **Push:**

   ```sh
   git remote add origin git@github.com:kenthejr/infinity-castle.git
   git push -u origin main
   ```

   The `build` workflow runs on every push. Check the *Actions* tab.

4. **Repository settings worth turning on:** a description and the topics `minecraft`, `fabric`, `minecraft-mod`,
   `procedural-generation`; *Settings → Branches → Add rule* for `main` requiring the `build` check; and *Settings →
   Security → Dependabot alerts*.

## Every release

1. Update `version` in `gradle.properties` and move the `CHANGELOG.md` entry out of *unreleased*.
2. Run the full check locally:

   ```sh
   ./gradlew build runClientGameTest
   ```

3. Commit, tag and push:

   ```sh
   git commit -am "Release 0.1.0"
   git tag -a v0.1.0 -m "Infinity Castle 0.1.0"
   git push && git push --tags
   ```

4. Create a GitHub release from the tag and attach `build/libs/infinity-castle-0.1.0.jar`. Don't attach the
   `-sources` jar as the download.

## Mod hosting sites

**Modrinth** (<https://modrinth.com/dashboard/projects>) and **CurseForge** (<https://authors.curseforge.com>) are
where players look for mods. For each:

- Create the project with *Fabric* as the loader, *Client and server* as the environment, and the *MIT* license.
  Link the GitHub repository as the source.
- Upload the jar for each release, marked for Minecraft 26.3, with Fabric API as a required dependency.
- Use the screenshots in `docs/images` and the icon at `src/main/resources/assets/infinitycastle/icon.png`.
- Keep the fan-work disclaimer from the README in the description. Don't use official artwork or logos from the show;
  the name "Infinity Castle" describing the castle is fine, but franchise art is not yours to redistribute.

Once this is routine, the upload can be automated from a GitHub Actions workflow triggered by tags (for example with
`Kir-Antipov/mc-publish`), using Modrinth and CurseForge API tokens stored as repository secrets.
