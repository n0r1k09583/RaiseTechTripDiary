(function () {
  var KEY = "rtl-proto-v1";
  var SESSION = "rtl-proto-session";

  function seed() {
    return {
      users: [
        { id: 1, username: "yamada", displayName: "山田", email: "yamada@example.com", password: "password123", bio: "課題の投稿係" },
        { id: 2, username: "hanako", displayName: "佐藤 花子", email: "hanako@example.com", password: "password123", bio: "いいねする人" },
        { id: 3, username: "ichiro", displayName: "鈴木 一郎", email: "ichiro@example.com", password: "password123", bio: "コメントする人" }
      ],
      posts: [
        { id: 1, userId: 2, body: "課題の要件定義、今日スタートした", image: "", createdAt: "2026-08-17T21:00:00" },
        { id: 2, userId: 3, body: "タイムラインの件数、一覧で見えないと困る", image: "", createdAt: "2026-08-17T20:45:00" },
        { id: 3, userId: 1, body: "HTML/CSS/JS のプロトタイプを先に固める", image: "", createdAt: "2026-08-17T22:10:00" }
      ],
      comments: [
        { id: 1, postId: 1, userId: 1, body: "画像も投稿できるようにした", createdAt: "2026-08-17T21:05:00" },
        { id: 2, postId: 1, userId: 3, body: "件数、タイムラインにも出して", createdAt: "2026-08-17T21:10:00" }
      ],
      likes: [
        { postId: 1, userId: 1 },
        { postId: 1, userId: 3 },
        { postId: 1, userId: 2 },
        { postId: 2, userId: 1 }
      ],
      follows: [
        { followerId: 2, followeeId: 1 }
      ],
      next: { user: 4, post: 4, comment: 3 }
    };
  }

  function load() {
    try {
      var raw = localStorage.getItem(KEY);
      if (!raw) {
        var s = seed();
        save(s);
        return s;
      }
      return JSON.parse(raw);
    } catch (e) {
      var s2 = seed();
      save(s2);
      return s2;
    }
  }

  function save(db) {
    localStorage.setItem(KEY, JSON.stringify(db));
  }

  var db = load();

  function currentUser() {
    var id = Number(sessionStorage.getItem(SESSION) || 0);
    return db.users.find(function (u) { return u.id === id; }) || null;
  }

  function setSession(id) {
    if (id) sessionStorage.setItem(SESSION, String(id));
    else sessionStorage.removeItem(SESSION);
  }

  function qs(name) {
    return new URLSearchParams(location.search).get(name) || "";
  }

  function requireAuth() {
    if (!currentUser()) location.href = "login.html";
  }

  function userById(id) {
    return db.users.find(function (u) { return u.id === id; });
  }

  function userByName(name) {
    name = String(name || "").toLowerCase();
    return db.users.find(function (u) { return u.username === name; });
  }

  function likeCount(postId) {
    return db.likes.filter(function (l) { return l.postId === postId; }).length;
  }

  function commentCount(postId) {
    return db.comments.filter(function (c) { return c.postId === postId; }).length;
  }

  function likedByMe(postId, uid) {
    return db.likes.some(function (l) { return l.postId === postId && l.userId === uid; });
  }

  function isFollowing(from, to) {
    return db.follows.some(function (f) { return f.followerId === from && f.followeeId === to; });
  }

  function followCount(uid) {
    return db.follows.filter(function (f) { return f.followerId === uid; }).length;
  }

  function followerCount(uid) {
    return db.follows.filter(function (f) { return f.followeeId === uid; }).length;
  }

  function fmt(iso) {
    var d = new Date(iso);
    if (isNaN(d)) return iso;
    var p = function (n) { return String(n).padStart(2, "0"); };
    return d.getFullYear() + "-" + p(d.getMonth() + 1) + "-" + p(d.getDate()) + " " + p(d.getHours()) + ":" + p(d.getMinutes());
  }

  function initial(u) {
    return (u.displayName || u.username).slice(0, 1);
  }

  function escapeHtml(s) {
    return String(s)
      .replace(/&/g, "&amp;")
      .replace(/</g, "&lt;")
      .replace(/>/g, "&gt;")
      .replace(/"/g, "&quot;");
  }

  function showModal(html) {
    var bg = document.getElementById("modal-bg");
    if (!bg) return;
    bg.querySelector(".modal").innerHTML = html;
    bg.classList.add("show");
  }

  function hideModal() {
    var bg = document.getElementById("modal-bg");
    if (bg) bg.classList.remove("show");
  }

  function bindModal() {
    var bg = document.getElementById("modal-bg");
    if (!bg) return;
    bg.addEventListener("click", function (e) {
      if (e.target === bg) hideModal();
    });
  }

  function headerHtml() {
    var me = currentUser();
    if (!me) return "";
    return (
      '<header class="topbar">' +
        '<a class="brand" href="timeline.html">課題提出<span class="brand-sub">タイムライン</span></a>' +
        '<form class="search" action="search.html" method="get">' +
          '<input type="text" name="q" placeholder="ユーザー名で検索" value="' + escapeHtml(qs("q")) + '" />' +
        "</form>" +
        '<div class="right">' +
          '<a class="chip" href="profile.html?user=' + me.username + '">@' + me.username + "</a>" +
          '<button type="button" class="btn ghost" id="logout-btn">ログアウト</button>' +
        "</div>" +
      "</header>" +
      '<nav class="nav-mini">' +
        '<a class="chip" href="timeline.html">タイムライン</a>' +
        '<a class="chip" href="post-new.html">投稿する</a>' +
        '<a class="chip" href="search.html">検索</a>' +
        '<a class="chip" href="profile.html?user=' + me.username + '">プロフィール</a>' +
      "</nav>"
    );
  }

  function postCard(post, me) {
    var u = userById(post.userId);
    var liked = likedByMe(post.id, me.id);
    var img = post.image
      ? '<img class="thumb" src="' + post.image + '" alt="投稿画像" data-open-image="' + post.id + '" />'
      : "";
    var edit = post.userId === me.id
      ? ' · <a href="post-edit.html?id=' + post.id + '">編集</a>'
      : "";
    return (
      '<article class="post" data-post="' + post.id + '">' +
        '<div class="avatar">' + escapeHtml(initial(u)) + "</div>" +
        "<div>" +
          '<div><a class="name" href="profile.html?user=' + u.username + '">' + escapeHtml(u.displayName) + "</a>" +
          '<a class="handle" href="profile.html?user=' + u.username + '">@' + u.username + "</a>" +
          '<span class="meta"> · ' + fmt(post.createdAt) + edit + "</span></div>" +
          '<p class="body">' + escapeHtml(post.body) + "</p>" +
          img +
          '<div class="stats">' +
            '<button type="button" class="like-btn' + (liked ? " liked" : "") + '" data-like="' + post.id + '">' +
              (liked ? "♥" : "♡") + " " + likeCount(post.id) +
            "</button>" +
            '<a href="post.html?id=' + post.id + '">コメント ' + commentCount(post.id) + "件</a>" +
          "</div>" +
        "</div>" +
      "</article>"
    );
  }

  function bindLikes() {
    document.querySelectorAll("[data-like]").forEach(function (btn) {
      btn.addEventListener("click", function () {
        var me = currentUser();
        if (!me) return;
        var id = Number(btn.getAttribute("data-like"));
        var idx = db.likes.findIndex(function (l) { return l.postId === id && l.userId === me.id; });
        if (idx >= 0) db.likes.splice(idx, 1);
        else db.likes.push({ postId: id, userId: me.id });
        save(db);
        location.reload();
      });
    });
    document.querySelectorAll("[data-open-image]").forEach(function (img) {
      img.addEventListener("click", function () {
        showModal("<h2>画像</h2><img src=\"" + img.src + "\" alt=\"拡大\" /><div class=\"row-actions\"><button class=\"btn ghost\" id=\"modal-close\">閉じる</button></div>");
        document.getElementById("modal-close").onclick = hideModal;
      });
    });
  }

  function bindLogout() {
    var btn = document.getElementById("logout-btn");
    if (!btn) return;
    btn.addEventListener("click", function () {
      showModal(
        "<h2>ログアウトしますか？</h2><p class=\"lead\">セッションを終了してログイン画面へ戻ります。</p>" +
        "<div class=\"row-actions\"><button class=\"btn ghost\" id=\"modal-cancel\">キャンセル</button>" +
        "<button class=\"btn danger\" id=\"modal-ok\">ログアウト</button></div>"
      );
      document.getElementById("modal-cancel").onclick = hideModal;
      document.getElementById("modal-ok").onclick = function () {
        setSession(null);
        location.href = "login.html";
      };
    });
  }

  function followButton(target, me) {
    if (!target || target.id === me.id) return "";
    var on = isFollowing(me.id, target.id);
    return '<button type="button" class="btn ' + (on ? "following" : "follow") + '" id="follow-btn" data-uid="' + target.id + '">' +
      (on ? "フォロー中" : "フォロー") + "</button>";
  }

  function bindFollow() {
    var btn = document.getElementById("follow-btn");
    if (!btn) return;
    btn.addEventListener("click", function () {
      var me = currentUser();
      var to = Number(btn.getAttribute("data-uid"));
      if (!me || me.id === to) return;
      var idx = db.follows.findIndex(function (f) { return f.followerId === me.id && f.followeeId === to; });
      if (idx >= 0) db.follows.splice(idx, 1);
      else db.follows.push({ followerId: me.id, followeeId: to });
      save(db);
      location.reload();
    });
  }

  function bindCounter(textarea, counter, max) {
    if (!textarea || !counter) return;
    function tick() {
      var n = textarea.value.length;
      counter.textContent = n + " / " + max;
      counter.classList.toggle("over", n > max);
    }
    textarea.addEventListener("input", tick);
    tick();
  }

  function setError(id, msg) {
    var el = document.getElementById(id);
    if (el) el.textContent = msg || "";
  }

  function validUsername(v) {
    return /^[a-z0-9_]{3,20}$/.test(v);
  }

  function pages() {
    var page = document.body.getAttribute("data-page");
    var shell = document.getElementById("shell");
    if (page !== "login" && page !== "signup") {
      requireAuth();
      if (shell) shell.insertAdjacentHTML("afterbegin", headerHtml(page));
      bindLogout();
    }
    bindModal();

    if (page === "login") loginPage();
    if (page === "signup") signupPage();
    if (page === "timeline") timelinePage();
    if (page === "post-new") postFormPage(false);
    if (page === "post-edit") postFormPage(true);
    if (page === "post") postDetailPage();
    if (page === "profile") profilePage();
    if (page === "profile-edit") profileEditPage();
    if (page === "search") searchPage();
    if (page === "following") followListPage("following");
    if (page === "followers") followListPage("followers");
  }

  function loginPage() {
    var form = document.getElementById("login-form");
    document.querySelectorAll("[data-demo]").forEach(function (b) {
      b.addEventListener("click", function () {
        document.getElementById("email").value = b.getAttribute("data-email");
        document.getElementById("password").value = "password123";
      });
    });
    form.addEventListener("submit", function (e) {
      e.preventDefault();
      var email = document.getElementById("email").value.trim();
      var password = document.getElementById("password").value;
      setError("email-error", "");
      setError("password-error", "");
      if (!email || !password) {
        if (!email) setError("email-error", "メールアドレスを入力してください");
        if (!password) setError("password-error", "パスワードを入力してください");
        return;
      }
      var u = db.users.find(function (x) { return x.email === email && x.password === password; });
      if (!u) {
        setError("password-error", "メールアドレスまたはパスワードが違います");
        document.getElementById("password").value = "";
        return;
      }
      setSession(u.id);
      location.href = "timeline.html";
    });
  }

  function signupPage() {
    var form = document.getElementById("signup-form");
    bindCounter(document.getElementById("displayName"), document.getElementById("name-count"), 20);
    form.addEventListener("submit", function (e) {
      e.preventDefault();
      var username = document.getElementById("username").value.trim().toLowerCase();
      var displayName = document.getElementById("displayName").value.trim();
      var email = document.getElementById("email").value.trim();
      var password = document.getElementById("password").value;
      var confirm = document.getElementById("confirm").value;
      ["username-error", "name-error", "email-error", "password-error", "confirm-error"].forEach(function (id) { setError(id, ""); });
      var ok = true;
      if (!validUsername(username)) { setError("username-error", "3〜20文字の半角英小文字・数字・_"); ok = false; }
      if (db.users.some(function (u) { return u.username === username; })) { setError("username-error", "このユーザー名は使われています"); ok = false; }
      if (!displayName || displayName.length > 20) { setError("name-error", "表示名は1〜20文字です"); ok = false; }
      if (!/^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(email)) { setError("email-error", "メール形式で入力してください"); ok = false; }
      if (db.users.some(function (u) { return u.email === email; })) { setError("email-error", "このメールは登録済みです"); ok = false; }
      if (password.length < 8) { setError("password-error", "8文字以上にしてください"); ok = false; }
      if (password !== confirm) { setError("confirm-error", "パスワードが一致しません"); ok = false; }
      if (!ok) return;
      var u = { id: db.next.user++, username: username, displayName: displayName, email: email, password: password, bio: "" };
      db.users.push(u);
      save(db);
      setSession(u.id);
      location.href = "timeline.html";
    });
  }

  function timelinePage() {
    var me = currentUser();
    var tab = qs("tab") === "following" ? "following" : "all";
    document.querySelectorAll(".tab").forEach(function (t) {
      t.classList.toggle("active", t.getAttribute("data-tab") === tab);
    });
    var list = db.posts.slice().sort(function (a, b) { return new Date(b.createdAt) - new Date(a.createdAt); });
    if (tab === "following") {
      var ids = db.follows.filter(function (f) { return f.followerId === me.id; }).map(function (f) { return f.followeeId; });
      ids.push(me.id);
      list = list.filter(function (p) { return ids.indexOf(p.userId) >= 0; });
    }
    var box = document.getElementById("feed");
    if (!list.length) {
      box.innerHTML = '<p class="empty">フォロー中の投稿はまだありません。プロフィールからフォローできます。</p>';
    } else {
      box.innerHTML = list.map(function (p) { return postCard(p, me); }).join("");
      bindLikes();
    }
  }

  function readImage(file, cb) {
    if (!file) { cb(""); return; }
    var ok = /image\/(jpeg|png|webp)/.test(file.type);
    if (!ok) { showModal("<h2>形式エラー</h2><p>JPEG / PNG / WebP のみです。</p><div class=\"row-actions\"><button class=\"btn\" id=\"modal-close\">閉じる</button></div>"); document.getElementById("modal-close").onclick = hideModal; cb(null); return; }
    if (file.size > 5 * 1024 * 1024) { showModal("<h2>サイズ超過</h2><p>5MBまでです。</p><div class=\"row-actions\"><button class=\"btn\" id=\"modal-close\">閉じる</button></div>"); document.getElementById("modal-close").onclick = hideModal; cb(null); return; }
    var reader = new FileReader();
    reader.onload = function () { cb(reader.result); };
    reader.readAsDataURL(file);
  }

  function postFormPage(isEdit) {
    var me = currentUser();
    var body = document.getElementById("body");
    bindCounter(body, document.getElementById("body-count"), 280);
    var post = null;
    if (isEdit) {
      post = db.posts.find(function (p) { return p.id === Number(qs("id")); });
      if (!post || post.userId !== me.id) {
        showModal("<h2>編集できません</h2><p>自分の投稿だけ編集できます。</p>");
        setTimeout(function () { location.href = "timeline.html"; }, 800);
        return;
      }
      body.value = post.body;
      document.getElementById("body-count").textContent = post.body.length + " / 280";
    }
    document.getElementById("post-form").addEventListener("submit", function (e) {
      e.preventDefault();
      var text = body.value.trim();
      setError("body-error", "");
      if (!text || text.length > 280) {
        setError("body-error", "本文は1〜280文字です");
        return;
      }
      var file = document.getElementById("image").files[0];
      var btn = document.querySelector("#post-form .btn");
      btn.disabled = true;
      readImage(file, function (data) {
        if (data === null) { btn.disabled = false; return; }
        if (isEdit) {
          post.body = text;
          if (data) post.image = data;
          save(db);
          location.href = "post.html?id=" + post.id;
        } else {
          var np = { id: db.next.post++, userId: me.id, body: text, image: data || "", createdAt: new Date().toISOString() };
          db.posts.push(np);
          save(db);
          location.href = "timeline.html";
        }
      });
    });
  }

  function postDetailPage() {
    var me = currentUser();
    var post = db.posts.find(function (p) { return p.id === Number(qs("id")); });
    var box = document.getElementById("detail");
    if (!post) { box.innerHTML = '<p class="empty">投稿が見つかりません。</p>'; return; }
    box.innerHTML = postCard(post, me);
    bindLikes();
    var comments = db.comments.filter(function (c) { return c.postId === post.id; });
    var list = document.getElementById("comments");
    if (!comments.length) list.innerHTML = '<p class="empty">まだコメントはありません（コメント 0件）</p>';
    else {
      list.innerHTML = comments.map(function (c) {
        var u = userById(c.userId);
        return '<div class="user-row"><div class="avatar">' + escapeHtml(initial(u)) + '</div><div class="grow">' +
          '<a class="name" href="profile.html?user=' + u.username + '">' + escapeHtml(u.displayName) + '</a> ' +
          '<a class="handle" href="profile.html?user=' + u.username + '">@' + u.username + '</a> ' +
          '<span class="meta">' + fmt(c.createdAt) + "</span>" +
          '<p class="body">' + escapeHtml(c.body) + "</p></div></div>";
      }).join("");
    }
    var ta = document.getElementById("comment-body");
    bindCounter(ta, document.getElementById("comment-count"), 140);
    document.getElementById("comment-form").addEventListener("submit", function (e) {
      e.preventDefault();
      var text = ta.value.trim();
      setError("comment-error", "");
      if (!text || text.length > 140) { setError("comment-error", "コメントは1〜140文字です"); return; }
      db.comments.push({ id: db.next.comment++, postId: post.id, userId: me.id, body: text, createdAt: new Date().toISOString() });
      save(db);
      location.reload();
    });
    var del = document.getElementById("delete-post");
    if (post.userId === me.id && del) {
      del.style.display = "inline-flex";
      del.addEventListener("click", function () {
        showModal("<h2>この投稿を削除しますか？</h2><p class=\"lead\">プロトタイプでは元に戻せません。</p>" +
          "<div class=\"row-actions\"><button class=\"btn ghost\" id=\"modal-cancel\">キャンセル</button>" +
          "<button class=\"btn danger\" id=\"modal-ok\">削除する</button></div>");
        document.getElementById("modal-cancel").onclick = hideModal;
        document.getElementById("modal-ok").onclick = function () {
          db.posts = db.posts.filter(function (p) { return p.id !== post.id; });
          db.comments = db.comments.filter(function (c) { return c.postId !== post.id; });
          db.likes = db.likes.filter(function (l) { return l.postId !== post.id; });
          save(db);
          location.href = "timeline.html";
        };
      });
    }
  }

  function profilePage() {
    var me = currentUser();
    var u = userByName(qs("user")) || me;
    document.getElementById("who").innerHTML =
      "<h1>" + escapeHtml(u.displayName) + "</h1>" +
      '<p class="lead">@' + u.username + " · " + escapeHtml(u.bio || "自己紹介はまだありません") + "</p>" +
      '<p><a href="following.html?user=' + u.username + '">フォロー ' + followCount(u.id) + "</a>　" +
      '<a href="followers.html?user=' + u.username + '">フォロワー ' + followerCount(u.id) + "</a></p>" +
      (u.id === me.id
        ? '<a class="btn" href="profile-edit.html">プロフィールを編集</a>'
        : followButton(u, me));
    bindFollow();
    var mePosts = db.posts.filter(function (p) { return p.userId === u.id; })
      .sort(function (a, b) { return new Date(b.createdAt) - new Date(a.createdAt); });
    document.getElementById("feed").innerHTML = mePosts.length
      ? mePosts.map(function (p) { return postCard(p, me); }).join("")
      : '<p class="empty">まだ投稿はありません。</p>';
    bindLikes();
  }

  function profileEditPage() {
    var me = currentUser();
    document.getElementById("displayName").value = me.displayName;
    document.getElementById("bio").value = me.bio || "";
    bindCounter(document.getElementById("displayName"), document.getElementById("name-count"), 20);
    bindCounter(document.getElementById("bio"), document.getElementById("bio-count"), 80);
    document.getElementById("profile-form").addEventListener("submit", function (e) {
      e.preventDefault();
      var name = document.getElementById("displayName").value.trim();
      var bio = document.getElementById("bio").value.trim();
      setError("name-error", "");
      if (!name || name.length > 20) { setError("name-error", "表示名は1〜20文字です"); return; }
      me.displayName = name;
      me.bio = bio.slice(0, 80);
      save(db);
      location.href = "profile.html?user=" + me.username;
    });
  }

  function searchPage() {
    var q = qs("q").trim().toLowerCase();
    document.getElementById("q").value = qs("q");
    var box = document.getElementById("results");
    if (!q) {
      box.innerHTML = '<p class="empty">ユーザー名を入力してください。</p>';
      return;
    }
    var hits = db.users.filter(function (u) { return u.username.indexOf(q) >= 0; });
    document.getElementById("result-label").textContent = "「" + qs("q") + "」の検索結果 " + hits.length + "件";
    if (!hits.length) {
      box.innerHTML = '<p class="empty">該当するユーザーはいません</p>';
      return;
    }
    var me = currentUser();
    box.innerHTML = hits.map(function (u) {
      return '<div class="user-row"><div class="avatar">' + escapeHtml(initial(u)) + '</div><div class="grow">' +
        '<a class="name" href="profile.html?user=' + u.username + '">' + escapeHtml(u.displayName) + "</a><br>" +
        '<a class="handle" href="profile.html?user=' + u.username + '">@' + u.username + "</a></div>" +
        (u.id === me.id ? "<span class=\"hint\">自分</span>" : '<a class="btn ghost" href="profile.html?user=' + u.username + '">プロフィール</a>') +
        "</div>";
    }).join("");
  }

  function followListPage(kind) {
    var me = currentUser();
    var u = userByName(qs("user")) || me;
    var ids = kind === "following"
      ? db.follows.filter(function (f) { return f.followerId === u.id; }).map(function (f) { return f.followeeId; })
      : db.follows.filter(function (f) { return f.followeeId === u.id; }).map(function (f) { return f.followerId; });
    document.getElementById("title").textContent = (kind === "following" ? "フォロー中" : "フォロワー") + " · @" + u.username;
    document.querySelectorAll(".tab").forEach(function (t) {
      t.classList.toggle("active", t.getAttribute("data-kind") === kind);
      t.href = t.getAttribute("data-kind") + ".html?user=" + u.username;
    });
    var box = document.getElementById("list");
    if (!ids.length) { box.innerHTML = '<p class="empty">まだいません</p>'; return; }
    box.innerHTML = ids.map(function (id) {
      var x = userById(id);
      var on = isFollowing(me.id, x.id);
      var btn = x.id === me.id ? "" : '<button type="button" class="btn ' + (on ? "following" : "follow") + '" data-uid="' + x.id + '">' + (on ? "フォロー中" : "フォロー") + "</button>";
      return '<div class="user-row"><div class="avatar">' + escapeHtml(initial(x)) + '</div><div class="grow">' +
        '<a class="name" href="profile.html?user=' + x.username + '">' + escapeHtml(x.displayName) + "</a> " +
        '<a class="handle" href="profile.html?user=' + x.username + '">@' + x.username + "</a></div>" + btn + "</div>";
    }).join("");
    box.querySelectorAll("[data-uid]").forEach(function (btn) {
      btn.addEventListener("click", function () {
        var to = Number(btn.getAttribute("data-uid"));
        var idx = db.follows.findIndex(function (f) { return f.followerId === me.id && f.followeeId === to; });
        if (idx >= 0) db.follows.splice(idx, 1);
        else db.follows.push({ followerId: me.id, followeeId: to });
        save(db);
        location.reload();
      });
    });
  }

  document.addEventListener("DOMContentLoaded", pages);
})();
